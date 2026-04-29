package com.ugurbuga.stackshift.localization

import androidx.compose.runtime.Composable
import com.ugurbuga.stackshift.game.model.GameplayStyle
import com.ugurbuga.stackshift.platform.GlobalPlatformConfig
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_title_blockwise
import stackshift.composeapp.generated.resources.app_title_stackshift

@Composable
fun appStringResource(resource: StringResource, vararg args: Any): String {
    return formatAppString(stringResource(resource, *args))
}

@Composable
fun appNameStringResource(): String {
    return stringResource(appNameResourceId())
}

fun appNameResourceId(): StringResource {
    return if (GlobalPlatformConfig.gameplayStyle == GameplayStyle.BlockWise) {
        Res.string.app_title_blockwise
    } else {
        Res.string.app_title_stackshift
    }
}

@Composable
fun formatAppString(raw: String): String {
    return if (GlobalPlatformConfig.gameplayStyle == GameplayStyle.BlockWise) {
        raw.replace(
            stringResource(Res.string.app_title_stackshift),
            stringResource(Res.string.app_title_blockwise)
        )
    } else {
        raw
    }
}
