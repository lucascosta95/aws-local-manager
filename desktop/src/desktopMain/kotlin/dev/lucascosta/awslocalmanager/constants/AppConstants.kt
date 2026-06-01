package dev.lucascosta.awslocalmanager.constants

object AppConstants {
    // General
    const val APP_NAME = "AWS Local Manager"
    const val EMPTY_STRING = ""
    const val UNKNOWN = "unknown"
    const val USER_HOME = "user.home"
    const val TIME_FORMAT_PATTERN = "HH:mm:ss"

    // AWS
    const val AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID"
    const val AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY"
    const val AWS_DEFAULT_REGION = "AWS_DEFAULT_REGION"
    const val AWS_ENDPOINT_URL = "AWS_ENDPOINT_URL"

    // Language
    const val ENGLISH = "en-US"
    const val PORTUGUESE = "pt-BR"

    // Labels
    const val SIDE_NAV_WIDTH = "side_nav_width"
    const val CONNECTION_DOT = "connection_dot"

    // Applications
    const val APPLICATION_OCTET_STREAM = "application/octet-stream"

    // Emulator
    const val FLOCI_IMAGE = "floci/floci:1.5.19"
    const val EMULATOR_CONTAINER_NAME = "aws-local-manager-emulator"
    const val EMULATOR_PORT_MAPPING = "4566:4566"
    const val EMULATOR_HEALTH_PATH = "/_floci/health"
    const val DOCKER_SOCKET_BINDING = "/var/run/docker.sock:/var/run/docker.sock"

    // App data
    const val APP_DATA_DIR_NAME = ".aws-local-manager"
    const val DATASTORE_FILENAME = "preferences.preferences_pb"
    const val PAYLOADS_FILENAME = "payloads.json"

    // Infrastructure
    const val TERRAFORM_FILE_EXTENSION = "tf"
    const val PROJECT_INFRA_SUBDIR = "infra"
    const val AWS_LOCAL_CONFIG_FILENAME = "aws-local.config.json"

    // Window
    const val WINDOW_WIDTH_DP = 1280
    const val WINDOW_HEIGHT_DP = 800

    // Preferences defaults
    const val DEFAULT_POLLING_INTERVAL_SECONDS = 10
    const val DEFAULT_MAX_HISTORY = 50

    // OS detection
    const val OS_NAME_PROPERTY = "os.name"
    const val OS_MAC_IDENTIFIER = "mac"
    const val COLIMA_COMMAND = "colima"

    // Resource prefixes / suffixes
    const val S3_URI_PREFIX = "s3://"
    const val DLQ_SUFFIX = "-DLQ"

    // AWS CLI attribute names
    const val SQS_REDRIVE_POLICY_ATTR = "RedrivePolicy"
    const val SQS_DLQ_TARGET_ARN_KEY = "deadLetterTargetArn"
    const val SQS_MAX_RECEIVE_COUNT_KEY = "maxReceiveCount"
    const val SNS_RAW_MESSAGE_DELIVERY_ATTR = "RawMessageDelivery"
    const val SNS_SUBSCRIPTION_ARN_KEY = "SubscriptionArn"

    // UI feedback delays (milliseconds)
    const val SAVED_FEEDBACK_DELAY_MS = 2_000L
    const val DELETION_SETTLE_DELAY_MS = 1_000L
    const val SQS_DLQ_CREATION_DELAY_MS = 500L
    const val DOCKER_READY_POLL_INTERVAL_MS = 4_000L
    const val FIX_SETTLE_DELAY_MS = 2_000L

    // Setup polling
    const val DOCKER_READY_POLL_MAX_ATTEMPTS = 15
    const val EMULATOR_READY_POLL_MAX_ATTEMPTS = 15

    // HTTP timeouts (milliseconds)
    const val HTTP_REQUEST_TIMEOUT_MS = 5_000L
    const val HTTP_CONNECT_TIMEOUT_MS = 3_000L

    // Health probe timeout (seconds)
    const val HEALTH_PROBE_TIMEOUT_SECONDS = 5L

    // Process timeout (seconds)
    const val PROCESS_DEFAULT_TIMEOUT_SECONDS = 30L

    // Setup diagnostics
    const val DOCKER_RUN_FAILED_MSG = "docker run: no output"

    // Update check
    const val UPDATE_CHECK_DELAY_MS = 3_000L
    const val MAX_RELEASE_NOTES_LENGTH = 300
}
