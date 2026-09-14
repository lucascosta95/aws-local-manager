package dev.lucascosta.awslocalmanager.domain

import aws_local_manager.desktop.generated.resources.Res
import aws_local_manager.desktop.generated.resources.aws_api_gateway
import aws_local_manager.desktop.generated.resources.aws_appconfig
import aws_local_manager.desktop.generated.resources.aws_application_integration
import aws_local_manager.desktop.generated.resources.aws_appsync
import aws_local_manager.desktop.generated.resources.aws_athena
import aws_local_manager.desktop.generated.resources.aws_auto_scaling
import aws_local_manager.desktop.generated.resources.aws_backup
import aws_local_manager.desktop.generated.resources.aws_batch
import aws_local_manager.desktop.generated.resources.aws_bedrock
import aws_local_manager.desktop.generated.resources.aws_bedrock_agentcore
import aws_local_manager.desktop.generated.resources.aws_certificate_manager
import aws_local_manager.desktop.generated.resources.aws_cloud_control_api
import aws_local_manager.desktop.generated.resources.aws_cloud_financial_management
import aws_local_manager.desktop.generated.resources.aws_cloud_map
import aws_local_manager.desktop.generated.resources.aws_cloudformation
import aws_local_manager.desktop.generated.resources.aws_cloudfront
import aws_local_manager.desktop.generated.resources.aws_cloudhsm
import aws_local_manager.desktop.generated.resources.aws_cloudtrail
import aws_local_manager.desktop.generated.resources.aws_cloudwatch
import aws_local_manager.desktop.generated.resources.aws_cloudwatch_logs
import aws_local_manager.desktop.generated.resources.aws_cloudwatch_rum
import aws_local_manager.desktop.generated.resources.aws_codebuild
import aws_local_manager.desktop.generated.resources.aws_codecommit
import aws_local_manager.desktop.generated.resources.aws_codedeploy
import aws_local_manager.desktop.generated.resources.aws_codeguru
import aws_local_manager.desktop.generated.resources.aws_codepipeline
import aws_local_manager.desktop.generated.resources.aws_cognito
import aws_local_manager.desktop.generated.resources.aws_comprehend
import aws_local_manager.desktop.generated.resources.aws_config
import aws_local_manager.desktop.generated.resources.aws_connect
import aws_local_manager.desktop.generated.resources.aws_control_tower
import aws_local_manager.desktop.generated.resources.aws_cost_and_usage_report
import aws_local_manager.desktop.generated.resources.aws_cost_explorer
import aws_local_manager.desktop.generated.resources.aws_data_firehose
import aws_local_manager.desktop.generated.resources.aws_documentdb
import aws_local_manager.desktop.generated.resources.aws_dynamodb
import aws_local_manager.desktop.generated.resources.aws_ec2
import aws_local_manager.desktop.generated.resources.aws_ec2_auto_scaling
import aws_local_manager.desktop.generated.resources.aws_ecr
import aws_local_manager.desktop.generated.resources.aws_ecs
import aws_local_manager.desktop.generated.resources.aws_efs
import aws_local_manager.desktop.generated.resources.aws_eks
import aws_local_manager.desktop.generated.resources.aws_elastic_beanstalk
import aws_local_manager.desktop.generated.resources.aws_elastic_cache
import aws_local_manager.desktop.generated.resources.aws_elastic_load_balancing
import aws_local_manager.desktop.generated.resources.aws_emr
import aws_local_manager.desktop.generated.resources.aws_eventbridge
import aws_local_manager.desktop.generated.resources.aws_eventbridge_pipes
import aws_local_manager.desktop.generated.resources.aws_eventbridge_scheduler
import aws_local_manager.desktop.generated.resources.aws_fault_injection_service
import aws_local_manager.desktop.generated.resources.aws_glue
import aws_local_manager.desktop.generated.resources.aws_guardduty
import aws_local_manager.desktop.generated.resources.aws_iam
import aws_local_manager.desktop.generated.resources.aws_iam_identity_center
import aws_local_manager.desktop.generated.resources.aws_iot_core
import aws_local_manager.desktop.generated.resources.aws_kinesis
import aws_local_manager.desktop.generated.resources.aws_kms
import aws_local_manager.desktop.generated.resources.aws_lake_formation
import aws_local_manager.desktop.generated.resources.aws_lambda
import aws_local_manager.desktop.generated.resources.aws_lightsail
import aws_local_manager.desktop.generated.resources.aws_managed_airflow
import aws_local_manager.desktop.generated.resources.aws_managed_flink
import aws_local_manager.desktop.generated.resources.aws_managed_prometheus
import aws_local_manager.desktop.generated.resources.aws_management_governance
import aws_local_manager.desktop.generated.resources.aws_memorydb
import aws_local_manager.desktop.generated.resources.aws_mq
import aws_local_manager.desktop.generated.resources.aws_msk
import aws_local_manager.desktop.generated.resources.aws_neptune
import aws_local_manager.desktop.generated.resources.aws_network_firewall
import aws_local_manager.desktop.generated.resources.aws_opensearch_service
import aws_local_manager.desktop.generated.resources.aws_organizations
import aws_local_manager.desktop.generated.resources.aws_rds
import aws_local_manager.desktop.generated.resources.aws_redshift
import aws_local_manager.desktop.generated.resources.aws_rekognition
import aws_local_manager.desktop.generated.resources.aws_resource_access_manager
import aws_local_manager.desktop.generated.resources.aws_resource_explorer
import aws_local_manager.desktop.generated.resources.aws_route_53
import aws_local_manager.desktop.generated.resources.aws_route_53_resolver
import aws_local_manager.desktop.generated.resources.aws_s3
import aws_local_manager.desktop.generated.resources.aws_s3_tables
import aws_local_manager.desktop.generated.resources.aws_s3_vectors
import aws_local_manager.desktop.generated.resources.aws_sagemaker
import aws_local_manager.desktop.generated.resources.aws_secrets_manager
import aws_local_manager.desktop.generated.resources.aws_service_catalog
import aws_local_manager.desktop.generated.resources.aws_ses
import aws_local_manager.desktop.generated.resources.aws_sns
import aws_local_manager.desktop.generated.resources.aws_sqs
import aws_local_manager.desktop.generated.resources.aws_step_functions
import aws_local_manager.desktop.generated.resources.aws_sts
import aws_local_manager.desktop.generated.resources.aws_support
import aws_local_manager.desktop.generated.resources.aws_systems_manager
import aws_local_manager.desktop.generated.resources.aws_textract
import aws_local_manager.desktop.generated.resources.aws_transcribe
import aws_local_manager.desktop.generated.resources.aws_transfer_family
import aws_local_manager.desktop.generated.resources.aws_waf
import aws_local_manager.desktop.generated.resources.aws_x_ray
import org.jetbrains.compose.resources.DrawableResource

enum class AwsServiceType(val displayName: String, val icon: DrawableResource, vararg val aliases: String) {
    ACM("Certificate Manager", Res.drawable.aws_certificate_manager),
    APIGATEWAY("API Gateway", Res.drawable.aws_api_gateway),
    APIGATEWAYV2("API Gateway V2", Res.drawable.aws_api_gateway),
    APPCONFIG("AppConfig", Res.drawable.aws_appconfig),
    APPCONFIGDATA("AppConfig Data", Res.drawable.aws_appconfig),
    APPLICATION_AUTOSCALING("Application Auto Scaling", Res.drawable.aws_auto_scaling, "APPLICATION-AUTOSCALING"),
    APPSYNC("AppSync", Res.drawable.aws_appsync),
    APS("Managed Prometheus", Res.drawable.aws_managed_prometheus),
    ATHENA("Athena", Res.drawable.aws_athena),
    AUTOSCALING("EC2 Auto Scaling", Res.drawable.aws_ec2_auto_scaling),
    BACKUP("Backup", Res.drawable.aws_backup),
    BATCH("Batch", Res.drawable.aws_batch),
    BCM_DATA_EXPORTS("Data Exports", Res.drawable.aws_cost_and_usage_report, "BCM-DATA-EXPORTS"),
    BEDROCK_AGENTCORE("Bedrock AgentCore", Res.drawable.aws_bedrock_agentcore, "BEDROCK-AGENTCORE"),
    BEDROCK_AGENTCORE_CONTROL("Bedrock AgentCore Control", Res.drawable.aws_bedrock_agentcore, "BEDROCK-AGENTCORE-CONTROL"),
    BEDROCK_RUNTIME("Bedrock Runtime", Res.drawable.aws_bedrock, "BEDROCK-RUNTIME", "BEDROCK"),
    CE("Cost Explorer", Res.drawable.aws_cost_explorer),
    CLOUDCONTROL("Cloud Control API", Res.drawable.aws_cloud_control_api),
    CLOUDFORMATION("CloudFormation", Res.drawable.aws_cloudformation),
    CLOUDFRONT("CloudFront", Res.drawable.aws_cloudfront),
    CLOUDHSMV2("CloudHSM", Res.drawable.aws_cloudhsm),
    CLOUDTRAIL("CloudTrail", Res.drawable.aws_cloudtrail),
    CLOUDWATCH("CloudWatch", Res.drawable.aws_cloudwatch, "MONITORING"),
    CLOUDWATCH_LOGS("CloudWatch Logs", Res.drawable.aws_cloudwatch_logs, "LOGS"),
    CODEBUILD("CodeBuild", Res.drawable.aws_codebuild),
    CODECOMMIT("CodeCommit", Res.drawable.aws_codecommit),
    CODEDEPLOY("CodeDeploy", Res.drawable.aws_codedeploy),
    CODEGURU_REVIEWER("CodeGuru Reviewer", Res.drawable.aws_codeguru, "CODEGURU-REVIEWER"),
    CODEPIPELINE("CodePipeline", Res.drawable.aws_codepipeline),
    COGNITO("Cognito", Res.drawable.aws_cognito, "COGNITO-IDP"),
    COMPREHEND("Comprehend", Res.drawable.aws_comprehend),
    CONFIG("Config", Res.drawable.aws_config),
    CONNECT("Connect", Res.drawable.aws_connect),
    CONTROLTOWER("Control Tower", Res.drawable.aws_control_tower),
    CUR("Cost and Usage Report", Res.drawable.aws_cost_and_usage_report),
    DOCDB("DocumentDB", Res.drawable.aws_documentdb),
    DYNAMODB("DynamoDB", Res.drawable.aws_dynamodb, "DYNAMODBSTREAMS"),
    EC2("EC2", Res.drawable.aws_ec2),
    ECR("ECR", Res.drawable.aws_ecr),
    ECS("ECS", Res.drawable.aws_ecs),
    EFS("EFS", Res.drawable.aws_efs),
    EKS("EKS", Res.drawable.aws_eks),
    ELASTICACHE("ElastiCache", Res.drawable.aws_elastic_cache),
    ELASTICBEANSTALK("Elastic Beanstalk", Res.drawable.aws_elastic_beanstalk),
    ELASTICLOADBALANCING("Elastic Load Balancing", Res.drawable.aws_elastic_load_balancing),
    ELB("Classic Load Balancer", Res.drawable.aws_elastic_load_balancing),
    EMR("EMR", Res.drawable.aws_emr, "ELASTICMAPREDUCE"),
    EMR_SERVERLESS("EMR Serverless", Res.drawable.aws_emr, "EMR-SERVERLESS"),
    EVENTBRIDGE("EventBridge", Res.drawable.aws_eventbridge, "EVENTS"),
    EVENTBRIDGE_PIPES("EventBridge Pipes", Res.drawable.aws_eventbridge_pipes, "PIPES"),
    EVENTBRIDGE_SCHEDULER("EventBridge Scheduler", Res.drawable.aws_eventbridge_scheduler, "SCHEDULER"),
    FIREHOSE("Data Firehose", Res.drawable.aws_data_firehose),
    FIS("Fault Injection Service", Res.drawable.aws_fault_injection_service),
    GLUE("Glue", Res.drawable.aws_glue),
    GUARDDUTY("GuardDuty", Res.drawable.aws_guardduty),
    IAM("IAM", Res.drawable.aws_iam),
    IAM_IDENTITY_CENTER("IAM Identity Center", Res.drawable.aws_iam_identity_center, "SSO"),
    IOT("IoT Core", Res.drawable.aws_iot_core),
    IOTDATA("IoT Data", Res.drawable.aws_iot_core),
    KINESIS("Kinesis", Res.drawable.aws_kinesis),
    KINESISANALYTICS("Managed Service for Apache Flink", Res.drawable.aws_managed_flink),
    KMS("KMS", Res.drawable.aws_kms),
    LAKEFORMATION("Lake Formation", Res.drawable.aws_lake_formation),
    LAMBDA("Lambda", Res.drawable.aws_lambda),
    LIGHTSAIL("Lightsail", Res.drawable.aws_lightsail),
    MEMORYDB("MemoryDB", Res.drawable.aws_memorydb),
    MQ("MQ", Res.drawable.aws_mq),
    MSK("MSK", Res.drawable.aws_msk, "KAFKA"),
    MWAA("Managed Airflow", Res.drawable.aws_managed_airflow),
    NEPTUNE("Neptune", Res.drawable.aws_neptune),
    NETWORK_FIREWALL("Network Firewall", Res.drawable.aws_network_firewall, "NETWORK-FIREWALL"),
    OPENSEARCH("OpenSearch Service", Res.drawable.aws_opensearch_service, "ES"),
    ORGANIZATIONS("Organizations", Res.drawable.aws_organizations),
    PRICING("Pricing", Res.drawable.aws_cloud_financial_management),
    RAM("Resource Access Manager", Res.drawable.aws_resource_access_manager),
    RDS("RDS", Res.drawable.aws_rds),
    RDS_DATA("RDS Data API", Res.drawable.aws_rds, "RDS-DATA"),
    REDSHIFT("Redshift", Res.drawable.aws_redshift),
    REKOGNITION("Rekognition", Res.drawable.aws_rekognition),
    RESOURCE_EXPLORER("Resource Explorer", Res.drawable.aws_resource_explorer, "RESOURCE-EXPLORER-2"),
    RESOURCE_GROUPS("Resource Groups", Res.drawable.aws_management_governance, "RESOURCE-GROUPS", "RESOURCEGROUPS"),
    RESOURCE_GROUPS_TAGGING("Resource Groups Tagging", Res.drawable.aws_management_governance, "TAGGING", "RESOURCEGROUPSTAGGINGAPI"),
    ROUTE53("Route 53", Res.drawable.aws_route_53),
    ROUTE53RESOLVER("Route 53 Resolver", Res.drawable.aws_route_53_resolver),
    RUM("CloudWatch RUM", Res.drawable.aws_cloudwatch_rum),
    S3("S3", Res.drawable.aws_s3, "S3CONTROL"),
    S3TABLES("S3 Tables", Res.drawable.aws_s3_tables),
    S3VECTORS("S3 Vectors", Res.drawable.aws_s3_vectors),
    SAGEMAKER("SageMaker", Res.drawable.aws_sagemaker),
    SECRETSMANAGER("Secrets Manager", Res.drawable.aws_secrets_manager),
    SERVICECATALOG("Service Catalog", Res.drawable.aws_service_catalog),
    SERVICEDISCOVERY("Cloud Map", Res.drawable.aws_cloud_map),
    SERVICEQUOTAS("Service Quotas", Res.drawable.aws_management_governance),
    SES("SES", Res.drawable.aws_ses, "EMAIL"),
    SNS("SNS", Res.drawable.aws_sns),
    SQS("SQS", Res.drawable.aws_sqs),
    SSM("Systems Manager", Res.drawable.aws_systems_manager),
    STEPFUNCTIONS("Step Functions", Res.drawable.aws_step_functions, "STATES"),
    STS("STS", Res.drawable.aws_sts),
    SUPPORT("Support", Res.drawable.aws_support),
    SWF("SWF", Res.drawable.aws_application_integration),
    TEXTRACT("Textract", Res.drawable.aws_textract),
    TRANSCRIBE("Transcribe", Res.drawable.aws_transcribe),
    TRANSFER("Transfer Family", Res.drawable.aws_transfer_family),
    WAFV2("WAF", Res.drawable.aws_waf),
    XRAY("X-Ray", Res.drawable.aws_x_ray),
    ;

    companion object {
        fun from(serviceName: String): AwsServiceType? {
            val key = serviceName.uppercase().trim()
            return entries.find { it.name == key || key in it.aliases }
        }
    }
}
