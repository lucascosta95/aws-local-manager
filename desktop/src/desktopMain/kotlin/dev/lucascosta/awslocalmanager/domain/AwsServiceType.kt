package dev.lucascosta.awslocalmanager.domain

import aws_local_manager.desktop.generated.resources.Res
import aws_local_manager.desktop.generated.resources.aws_api_gateway
import aws_local_manager.desktop.generated.resources.aws_athena
import aws_local_manager.desktop.generated.resources.aws_certificate_manager
import aws_local_manager.desktop.generated.resources.aws_cloudformation
import aws_local_manager.desktop.generated.resources.aws_cloudfront
import aws_local_manager.desktop.generated.resources.aws_cloudwatch
import aws_local_manager.desktop.generated.resources.aws_codebuild
import aws_local_manager.desktop.generated.resources.aws_codecommit
import aws_local_manager.desktop.generated.resources.aws_codepipeline
import aws_local_manager.desktop.generated.resources.aws_cognito
import aws_local_manager.desktop.generated.resources.aws_config
import aws_local_manager.desktop.generated.resources.aws_data_firehose
import aws_local_manager.desktop.generated.resources.aws_dynamodb
import aws_local_manager.desktop.generated.resources.aws_ec2
import aws_local_manager.desktop.generated.resources.aws_ecr
import aws_local_manager.desktop.generated.resources.aws_ecs
import aws_local_manager.desktop.generated.resources.aws_eks
import aws_local_manager.desktop.generated.resources.aws_elastic_cache
import aws_local_manager.desktop.generated.resources.aws_email
import aws_local_manager.desktop.generated.resources.aws_emr
import aws_local_manager.desktop.generated.resources.aws_eventbridge
import aws_local_manager.desktop.generated.resources.aws_glue
import aws_local_manager.desktop.generated.resources.aws_iam
import aws_local_manager.desktop.generated.resources.aws_iam_identity_center
import aws_local_manager.desktop.generated.resources.aws_kinesis
import aws_local_manager.desktop.generated.resources.aws_kms
import aws_local_manager.desktop.generated.resources.aws_lambda
import aws_local_manager.desktop.generated.resources.aws_opensearch_service
import aws_local_manager.desktop.generated.resources.aws_rds
import aws_local_manager.desktop.generated.resources.aws_redshift
import aws_local_manager.desktop.generated.resources.aws_resource_explorer
import aws_local_manager.desktop.generated.resources.aws_route_53
import aws_local_manager.desktop.generated.resources.aws_s3
import aws_local_manager.desktop.generated.resources.aws_sagemaker
import aws_local_manager.desktop.generated.resources.aws_secrets_manager
import aws_local_manager.desktop.generated.resources.aws_ses
import aws_local_manager.desktop.generated.resources.aws_sns
import aws_local_manager.desktop.generated.resources.aws_sqs
import aws_local_manager.desktop.generated.resources.aws_step_functions
import aws_local_manager.desktop.generated.resources.aws_support
import aws_local_manager.desktop.generated.resources.aws_systems_manager
import aws_local_manager.desktop.generated.resources.aws_transcribe
import aws_local_manager.desktop.generated.resources.aws_x_ray
import org.jetbrains.compose.resources.DrawableResource

enum class AwsServiceType(val icon: DrawableResource, vararg val aliases: String) {
    ACM(Res.drawable.aws_certificate_manager),
    APIGATEWAY(Res.drawable.aws_api_gateway, "APIGATEWAYV2"),
    ATHENA(Res.drawable.aws_athena),
    CLOUDFORMATION(Res.drawable.aws_cloudformation),
    CLOUDFRONT(Res.drawable.aws_cloudfront),
    CLOUDWATCH(Res.drawable.aws_cloudwatch, "LOGS", "MONITORING"),
    CODECOMMIT(Res.drawable.aws_codecommit),
    CODEBUILD(Res.drawable.aws_codebuild),
    CODEPIPELINE(Res.drawable.aws_codepipeline),
    COGNITO(Res.drawable.aws_cognito, "COGNITO-IDP"),
    CONFIG(Res.drawable.aws_config, "APPCONFIG", "APPCONFIGDATA"),
    DYNAMODB(Res.drawable.aws_dynamodb, "DYNAMODBSTREAMS"),
    EC2(Res.drawable.aws_ec2),
    ECS(Res.drawable.aws_ecs),
    EKS(Res.drawable.aws_eks),
    EMR(Res.drawable.aws_emr),
    ECR(Res.drawable.aws_ecr),
    ELASTICACHE(Res.drawable.aws_elastic_cache),
    EMAIL(Res.drawable.aws_email),
    OPENSEARCH(Res.drawable.aws_opensearch_service, "ES"),
    EVENTS(Res.drawable.aws_eventbridge, "SCHEDULER"),
    FIREHOSE(Res.drawable.aws_data_firehose),
    GLUE(Res.drawable.aws_glue),
    IAM(Res.drawable.aws_iam),
    KINESIS(Res.drawable.aws_kinesis),
    KMS(Res.drawable.aws_kms),
    LAMBDA(Res.drawable.aws_lambda),
    RDS(Res.drawable.aws_rds),
    REDSHIFT(Res.drawable.aws_redshift),
    RESOURCEGROUPS(Res.drawable.aws_resource_explorer, "RESOURCE-GROUPS", "RESOURCEGROUPSTAGGINGAPI"),
    ROUTE53(Res.drawable.aws_route_53, "ROUTE53RESOLVER"),
    S3(Res.drawable.aws_s3, "S3CONTROL"),
    SAGEMAKER(Res.drawable.aws_sagemaker),
    SECRETSMANAGER(Res.drawable.aws_secrets_manager),
    SES(Res.drawable.aws_ses),
    SNS(Res.drawable.aws_sns),
    SQS(Res.drawable.aws_sqs),
    SSM(Res.drawable.aws_systems_manager),
    STEPFUNCTIONS(Res.drawable.aws_step_functions, "SWF", "STATES"),
    STS(Res.drawable.aws_iam_identity_center),
    SUPPORT(Res.drawable.aws_support),
    TRANSCRIBE(Res.drawable.aws_transcribe),
    XRAY(Res.drawable.aws_x_ray),
    ;

    companion object {
        fun from(serviceName: String): AwsServiceType? {
            val key = serviceName.uppercase().trim()
            return entries.find { it.name == key || key in it.aliases }
        }
    }
}
