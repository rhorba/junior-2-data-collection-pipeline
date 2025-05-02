# provider.tf
provider "aws" {
  region = "us-east-1"
}

# variables.tf
variable "project_name" {
  description = "Name of the project"
  default     = "data-pipeline"
}

variable "environment" {
  description = "Environment (dev, prod, etc.)"
  default     = "dev"
}

variable "api_weather_key" {
  description = "API key for OpenWeatherMap"
  default     = "" # Use AWS Secrets Manager in production
}

variable "db_username" {
  description = "Database username"
  type        = string
  default     = "admin"
  sensitive   = true
}

variable "db_password" {
  description = "Database password"
  type        = string
  default     = "password" # For development only
  sensitive   = true
}

variable "kafka_username" {
  description = "Kafka username"
  type        = string
  default     = "admin"
  sensitive   = true
}

variable "kafka_password" {
  description = "Kafka password"
  type        = string
  default     = "password" # For development only
  sensitive   = true
}

# network.tf - VPC and Networking
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"

  name = "${var.project_name}-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["us-east-1a", "us-east-1b", "us-east-1c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  single_nat_gateway = true

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# EFS for source code mounting (defined early for dependency clarity)
resource "aws_efs_file_system" "app_efs" {
  creation_token = "${var.project_name}-efs"

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_security_group" "efs_sg" {
  name        = "efs-sg"
  description = "Security group for EFS"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port   = 2049
    to_port     = 2049
    protocol    = "tcp"
    cidr_blocks = [module.vpc.vpc_cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_efs_mount_target" "app_efs_mount" {
  count           = length(module.vpc.private_subnets)
  file_system_id  = aws_efs_file_system.app_efs.id
  subnet_id       = module.vpc.private_subnets[count.index]
  security_groups = [aws_security_group.efs_sg.id]
}

# kafka.tf - MSK Cluster
resource "aws_security_group" "kafka_sg" {
  name        = "kafka-sg"
  description = "Security group for Kafka cluster"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port   = 9092
    to_port     = 9092
    protocol    = "tcp"
    cidr_blocks = [module.vpc.vpc_cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_msk_configuration" "kafka_config" {
  name              = "${var.project_name}-kafka-config"
  kafka_versions    = ["3.4.0"] # Updated to the latest version
  server_properties = <<PROPERTIES
auto.create.topics.enable=true
delete.topic.enable=true
offsets.topic.replication.factor=1
PROPERTIES
}

resource "aws_msk_cluster" "kafka_cluster" {
  cluster_name           = "${var.project_name}-kafka"
  kafka_version          = "3.4.0" # Updated to the latest version
  number_of_broker_nodes = 3

  configuration_info {
    arn      = aws_msk_configuration.kafka_config.arn
    revision = aws_msk_configuration.kafka_config.latest_revision
  }

  broker_node_group_info {
    instance_type   = "kafka.t3.small"
    client_subnets  = module.vpc.private_subnets
    security_groups = [aws_security_group.kafka_sg.id]
    storage_info {
      ebs_storage_info {
        volume_size = 10
      }
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS_PLAINTEXT"
    }
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# database.tf - RDS Database
resource "aws_security_group" "db_sg" {
  name        = "db-sg"
  description = "Security group for RDS instance"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port   = 3306
    to_port     = 3306
    protocol    = "tcp"
    cidr_blocks = [module.vpc.vpc_cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_db_subnet_group" "db_subnet_group" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = module.vpc.private_subnets

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_db_instance" "weather_db" {
  identifier           = "${var.project_name}-db"
  allocated_storage    = 20
  storage_type         = "gp2"
  engine               = "mysql"
  engine_version       = "8.0"
  instance_class       = "db.t3.micro"
  db_name              = "weatherdb"
  username             = var.db_username
  password             = var.db_password
  parameter_group_name = "default.mysql8.0"
  skip_final_snapshot  = true

  vpc_security_group_ids = [aws_security_group.db_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.db_subnet_group.name

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# ecs.tf - ECS Cluster and Service
resource "aws_ecs_cluster" "app_cluster" {
  name = "${var.project_name}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_ecr_repository" "app_repo" {
  name                 = var.project_name
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_cloudwatch_log_group" "app_logs" {
  name              = "/ecs/${var.project_name}"
  retention_in_days = 30

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# IAM roles for ECS
resource "aws_iam_role" "ecs_execution_role" {
  name = "${var.project_name}-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_iam_role_policy_attachment" "ecs_execution_role_policy" {
  role       = aws_iam_role.ecs_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "ecs_task_role" {
  name = "${var.project_name}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Security group for the app
resource "aws_security_group" "app_sg" {
  name        = "app-sg"
  description = "Security group for application"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [module.vpc.vpc_cidr_block]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# ALB Security Group
resource "aws_security_group" "lb_sg" {
  name        = "lb-sg"
  description = "Security group for ALB"
  vpc_id      = module.vpc.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # Added GraphiQL access
  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Application Load Balancer
resource "aws_lb" "app_lb" {
  name               = "${var.project_name}-lb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.lb_sg.id]
  subnets            = module.vpc.public_subnets

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# ALB Target Group
resource "aws_lb_target_group" "app_tg" {
  name        = "${var.project_name}-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = module.vpc.vpc_id
  target_type = "ip"

  health_check {
    path                = "/actuator/health"
    port                = "traffic-port"
    healthy_threshold   = 3
    unhealthy_threshold = 3
    timeout             = 5
    interval            = 30
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# ALB Listener - Removed unsupported tags
resource "aws_lb_listener" "app_listener" {
  load_balancer_arn = aws_lb.app_lb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app_tg.arn
  }
}

# GraphiQL Listener - Removed unsupported tags
resource "aws_lb_listener" "graphiql_listener" {
  load_balancer_arn = aws_lb.app_lb.arn
  port              = 8080
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.app_tg.arn
  }
}

# ECS Task Definition
resource "aws_ecs_task_definition" "app_task" {
  family                   = "${var.project_name}-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512" # Increased from 256 to 512
  memory                   = "1024" # Increased from 512 to 1024
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name  = "${var.project_name}-app"
      image = "${aws_ecr_repository.app_repo.repository_url}:latest"
      essential = true
      environment = [
        { name = "SPRING_KAFKA_BOOTSTRAP_SERVERS", value = aws_msk_cluster.kafka_cluster.bootstrap_brokers },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:mysql://${aws_db_instance.weather_db.endpoint}/weatherdb" },
        { name = "SPRING_DATASOURCE_USERNAME", value = var.db_username },
        { name = "SPRING_DATASOURCE_PASSWORD", value = var.db_password },
        { name = "SPRING_DATASOURCE_DRIVER", value = "com.mysql.cj.jdbc.Driver" },
        { name = "SPRING_JPA_DATABASE_PLATFORM", value = "org.hibernate.dialect.MySQLDialect" },
        { name = "API_WEATHER_KEY", value = var.api_weather_key },
        { name = "SPRING_DEVTOOLS_RESTART_ENABLED", value = "true" }
      ]
      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app_logs.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "ecs"
        }
      }
      mountPoints = [
        {
          sourceVolume  = "source-code"
          containerPath = "/app/src"
          readOnly      = false
        }
      ]
    }
  ])

  volume {
    name = "source-code"
    efs_volume_configuration {
      file_system_id = aws_efs_file_system.app_efs.id
      root_directory = "/"
    }
  }

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# ECS Service
resource "aws_ecs_service" "app_service" {
  name            = "${var.project_name}-service"
  cluster         = aws_ecs_cluster.app_cluster.id
  task_definition = aws_ecs_task_definition.app_task.arn
  desired_count   = 2 # Increased from 1 to 2 for high availability
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = module.vpc.private_subnets
    security_groups  = [aws_security_group.app_sg.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.app_tg.arn
    container_name   = "${var.project_name}-app"
    container_port   = 8080
  }

  depends_on = [
    aws_lb_listener.app_listener,
    aws_lb_listener.graphiql_listener
  ]

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

# Auto Scaling for ECS
resource "aws_appautoscaling_target" "ecs_target" {
  max_capacity       = 4
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.app_cluster.name}/${aws_ecs_service.app_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "ecs_policy_cpu" {
  name               = "${var.project_name}-cpu-autoscaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 70.0
  }
}

resource "aws_appautoscaling_policy" "ecs_policy_memory" {
  name               = "${var.project_name}-memory-autoscaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }
    target_value = 70.0
  }
}

# Kafka Topic Creation
resource "aws_msk_scram_secret_association" "kafka_topic_creator" {
  cluster_arn     = aws_msk_cluster.kafka_cluster.arn
  secret_arn_list = [aws_secretsmanager_secret.kafka_password.arn]
}

resource "aws_secretsmanager_secret" "kafka_password" {
  name = "${var.project_name}-kafka-secret"

  tags = {
    Environment = var.environment
    Project     = var.project_name
  }
}

resource "aws_secretsmanager_secret_version" "kafka_password" {
  secret_id     = aws_secretsmanager_secret.kafka_password.id
  secret_string = jsonencode({
    username = var.kafka_username
    password = var.kafka_password
  })
}

# Required IAM policy for MSK topic creation
resource "aws_iam_policy" "kafka_topic_policy" {
  name        = "${var.project_name}-kafka-topic-policy"
  description = "Policy for Kafka topic creation"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "kafka:CreateTopic",
          "kafka:DescribeTopic",
          "kafka:DeleteTopic"
        ]
        Resource = "*"
      }
    ]
  })
}

# Attach the policy to the ECS task role
resource "aws_iam_role_policy_attachment" "ecs_kafka_topic_policy" {
  role       = aws_iam_role.ecs_task_role.name
  policy_arn = aws_iam_policy.kafka_topic_policy.arn
}

# outputs.tf - Outputs
output "kafka_bootstrap_servers" {
  description = "Kafka bootstrap servers"
  value       = aws_msk_cluster.kafka_cluster.bootstrap_brokers
}

output "db_endpoint" {
  description = "Database endpoint"
  value       = aws_db_instance.weather_db.endpoint
}

output "app_url" {
  description = "Application URL"
  value       = "http://${aws_lb.app_lb.dns_name}"
}

output "graphiql_url" {
  description = "GraphiQL Interface URL"
  value       = "http://${aws_lb.app_lb.dns_name}:8080/graphiql"
}

output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = aws_ecr_repository.app_repo.repository_url
}
