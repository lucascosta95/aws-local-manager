package dev.lucascosta.awslocalmanager.i18n

import androidx.compose.runtime.compositionLocalOf
import dev.lucascosta.awslocalmanager.constants.AppConstants.ENGLISH

val LocalSkillsStrings = compositionLocalOf<SkillsStrings> { SkillsStringsPtBr }

fun skillsStringsForLanguage(tag: String): SkillsStrings =
    when (tag) {
        ENGLISH -> SkillsStringsEnUs
        else -> SkillsStringsPtBr
    }

data class SkillsStrings(
    val skillsTitle: String,
    val skillsSubtitle: String,
    val skillsRefresh: String,
    val skillsSourceRemote: String,
    val skillsSourceBundled: String,
    val skillsEmpty: String,
    val skillsSelectHint: String,
    val skillsVersionFmt: String,
    val skillsTargets: String,
    val skillsTargetsHint: String,
    val skillsDetected: String,
    val skillsNotDetected: String,
    val skillsInstalledBadge: String,
    val skillsUpdateBadge: String,
    val skillsInstall: String,
    val skillsUninstall: String,
    val skillsPreview: String,
    val skillsPreviewTitle: String,
    val skillsClose: String,
    val skillsCancel: String,
    val skillsConfirmTitle: String,
    val skillsConfirmMessage: String,
    val skillsConfirmWarning: String,
    val skillsInstalledFmt: String,
    val skillsRemovedFmt: String,
    val skillsFailedFmt: String,
    val skillsErrorContent: String,
)

val SkillsStringsEnUs =
    SkillsStrings(
        skillsTitle = "Skills",
        skillsSubtitle = "Install these instructions into your AI coding tools, so they can prepare a project for local debugging.",
        skillsRefresh = "Refresh catalog",
        skillsSourceRemote = "Online catalog",
        skillsSourceBundled = "Bundled catalog",
        skillsEmpty = "No skills available",
        skillsSelectHint = "Select a skill to see where it can be installed",
        skillsVersionFmt = "version {version}",
        skillsTargets = "Tools",
        skillsTargetsHint = "Only tools detected in your home folder are selected by default.",
        skillsDetected = "Detected",
        skillsNotDetected = "Not detected",
        skillsInstalledBadge = "Installed",
        skillsUpdateBadge = "Update available",
        skillsInstall = "Install",
        skillsUninstall = "Remove",
        skillsPreview = "View content",
        skillsPreviewTitle = "Skill content",
        skillsClose = "Close",
        skillsCancel = "Cancel",
        skillsConfirmTitle = "Confirm installation",
        skillsConfirmMessage = "These files will be written:",
        skillsConfirmWarning = "Shared instruction files are edited inside a delimited block and a .bak copy is kept.",
        skillsInstalledFmt = "{name} installed in {count} location(s)",
        skillsRemovedFmt = "{name} removed from {count} location(s)",
        skillsFailedFmt = "Failed for: {targets}",
        skillsErrorContent = "Could not load the skill content",
    )

val SkillsStringsPtBr =
    SkillsStrings(
        skillsTitle = "Skills",
        skillsSubtitle = "Instale estas instruções nas suas ferramentas de IA, para que elas preparem um projeto para debug local.",
        skillsRefresh = "Atualizar catálogo",
        skillsSourceRemote = "Catálogo online",
        skillsSourceBundled = "Catálogo local",
        skillsEmpty = "Nenhuma skill disponível",
        skillsSelectHint = "Selecione uma skill para ver onde ela pode ser instalada",
        skillsVersionFmt = "versão {version}",
        skillsTargets = "Ferramentas",
        skillsTargetsHint = "Só as ferramentas detectadas na sua pasta pessoal vêm marcadas por padrão.",
        skillsDetected = "Detectada",
        skillsNotDetected = "Não detectada",
        skillsInstalledBadge = "Instalada",
        skillsUpdateBadge = "Atualização disponível",
        skillsInstall = "Instalar",
        skillsUninstall = "Remover",
        skillsPreview = "Ver conteúdo",
        skillsPreviewTitle = "Conteúdo da skill",
        skillsClose = "Fechar",
        skillsCancel = "Cancelar",
        skillsConfirmTitle = "Confirmar instalação",
        skillsConfirmMessage = "Estes arquivos serão gravados:",
        skillsConfirmWarning = "Arquivos de instrução compartilhados recebem um bloco delimitado, e uma cópia .bak é mantida.",
        skillsInstalledFmt = "{name} instalada em {count} local(is)",
        skillsRemovedFmt = "{name} removida de {count} local(is)",
        skillsFailedFmt = "Falhou em: {targets}",
        skillsErrorContent = "Não foi possível carregar o conteúdo da skill",
    )
