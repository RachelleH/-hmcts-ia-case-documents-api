# Temporary fix for template API version error on deployment
provider "azurerm" {
  version = "1.21.0"
}

locals {

  preview_app_service_plan     = "${var.product}-${var.component}-${var.env}"
  non_preview_app_service_plan = "${var.product}-${var.env}"
  app_service_plan             = "${var.env == "preview" || var.env == "spreview" ? local.preview_app_service_plan : local.non_preview_app_service_plan}"

  preview_vault_name           = "${var.raw_product}-aat"
  non_preview_vault_name       = "${var.raw_product}-${var.env}"
  key_vault_name               = "${var.env == "preview" || var.env == "spreview" ? local.preview_vault_name : local.non_preview_vault_name}"

  docmosis_prod_key_vault_name = "docmosisiaasprodkv"
  docmosis_dev_key_vault_name  = "docmosisiaasdevkv"
  docmosis_key_vault_name      = "${var.env == "prod" ? local.docmosis_prod_key_vault_name : local.docmosis_dev_key_vault_name}"
  docmosis_key_vault_uri       = "https://${local.docmosis_key_vault_name}.vault.azure.net/"
}

resource "azurerm_resource_group" "rg" {
  name     = "${var.product}-${var.component}-${var.env}"
  location = "${var.location}"
  tags     = "${merge(var.common_tags, map("lastUpdated", "${timestamp()}"))}"
}

data "azurerm_key_vault" "ia_key_vault" {
  name                = "${local.key_vault_name}"
  resource_group_name = "${local.key_vault_name}"
}

data "azurerm_key_vault_secret" "appeal_submission_template" {
  name      = "appeal-submission-template"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "docmosis_api_key" {
  name      = "docmosis-api-key"
  vault_uri = "${local.docmosis_key_vault_uri}"
}

data "azurerm_key_vault_secret" "docmosis_endpoint" {
  name      = "docmosis-endpoint"
  vault_uri = "${local.docmosis_key_vault_uri}"
}

data "azurerm_key_vault_secret" "test_caseofficer_username" {
  name      = "test-caseofficer-username"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "test_caseofficer_password" {
  name      = "test-caseofficer-password"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "test_law_firm_a_username" {
  name      = "test-law-firm-a-username"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "test_law_firm_a_password" {
  name      = "test-law-firm-a-password"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "system_username" {
  name      = "system-username"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "system_password" {
  name      = "system-password"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_client_id" {
  name      = "idam-client-id"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_secret" {
  name      = "idam-secret"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_redirect_uri" {
  name      = "idam-redirect-uri"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "s2s_secret" {
  name      = "s2s-secret"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "s2s_microservice" {
  name      = "s2s-microservice"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "ccd_url" {
  name      = "ccd-url"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "ccd_gw_url" {
  name      = "ccd-gw-url"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "dm_url" {
  name      = "dm-url"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "idam_url" {
  name      = "idam-url"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "s2s_url" {
  name      = "s2s-url"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "em_bundler_url" {
  name      = "em-bundler-url"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_key_vault_secret" "em_bundler_stitch_uri" {
  name      = "em-bundler-stitch-uri"
  vault_uri = "${data.azurerm_key_vault.ia_key_vault.vault_uri}"
}

data "azurerm_lb" "consul_dns" {
  name                = "consul-server_dns"
  resource_group_name = "${var.consul_dns_resource_group_name}"
}

module "ia_case_documents_api" {
  source                          = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product                         = "${var.product}-${var.component}"
  location                        = "${var.location}"
  env                             = "${var.env}"
  ilbIp                           = "${var.ilbIp}"
  resource_group_name             = "${azurerm_resource_group.rg.name}"
  subscription                    = "${var.subscription}"
  capacity                        = "${var.capacity}"
  instance_size                   = "${var.instance_size}"
  common_tags                     = "${merge(var.common_tags, map("lastUpdated", "${timestamp()}"))}"
  appinsights_instrumentation_key = "${var.appinsights_instrumentation_key}"
  asp_name                        = "${local.app_service_plan}"
  asp_rg                          = "${local.app_service_plan}"

  app_settings = {
    LOGBACK_REQUIRE_ALERT_LEVEL = false
    LOGBACK_REQUIRE_ERROR_CODE  = false

    WEBSITE_DNS_SERVER = "${data.azurerm_lb.consul_dns.private_ip_address}"

    DOCMOSIS_ACCESS_KEY = "${data.azurerm_key_vault_secret.docmosis_api_key.value}"
    DOCMOSIS_ENDPOINT   = "${data.azurerm_key_vault_secret.docmosis_endpoint.value}"

    IA_APPEAL_SUBMISSION_TEMPLATE = "${data.azurerm_key_vault_secret.appeal_submission_template.value}"

    IA_SYSTEM_USERNAME   = "${data.azurerm_key_vault_secret.system_username.value}"
    IA_SYSTEM_PASSWORD   = "${data.azurerm_key_vault_secret.system_password.value}"
    IA_IDAM_CLIENT_ID    = "${data.azurerm_key_vault_secret.idam_client_id.value}"
    IA_IDAM_SECRET       = "${data.azurerm_key_vault_secret.idam_secret.value}"
    IA_IDAM_REDIRECT_URI = "${data.azurerm_key_vault_secret.idam_redirect_uri.value}"
    IA_S2S_SECRET        = "${data.azurerm_key_vault_secret.s2s_secret.value}"
    IA_S2S_MICROSERVICE  = "${data.azurerm_key_vault_secret.s2s_microservice.value}"

    CCD_URL    = "${data.azurerm_key_vault_secret.ccd_url.value}"
    CCD_GW_URL = "${data.azurerm_key_vault_secret.ccd_gw_url.value}"
    DM_URL     = "${data.azurerm_key_vault_secret.dm_url.value}"
    IDAM_URL   = "${data.azurerm_key_vault_secret.idam_url.value}"
    S2S_URL    = "${data.azurerm_key_vault_secret.s2s_url.value}"

    EM_BUNDLER_URL           = "${data.azurerm_key_vault_secret.em_bundler_url.value}"
    EM_BUNDLER_STITCH_URI    = "${data.azurerm_key_vault_secret.em_bundler_stitch_uri.value}"

    ROOT_LOGGING_LEVEL   = "${var.root_logging_level}"
    LOG_LEVEL_SPRING_WEB = "${var.log_level_spring_web}"
    LOG_LEVEL_IA         = "${var.log_level_ia}"
  }
}
