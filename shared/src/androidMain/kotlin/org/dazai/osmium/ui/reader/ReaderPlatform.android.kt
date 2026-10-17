package org.dazai.osmium.ui.reader

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

import androidx.compose.ui.graphics.toArgb

@Composable
actual fun getReaderPlatformSettings(primaryColor: androidx.compose.ui.graphics.Color): ReaderPlatformSettings {
    val isOldAndroid = Build.VERSION.SDK_INT <= 24
    val primaryColorHex = String.format("#%06X", (0xFFFFFF and primaryColor.toArgb()))

    val script = remember(isOldAndroid, primaryColorHex) {
        """
    (function() {
        var primaryColor = '$primaryColorHex';
        
        var css = 'img { max-width: 100% !important}';
        css += ':root { --osmium-primary-color: ' + primaryColor + '; }';

        function injectStyle() {
            var head = document.head || document.getElementsByTagName('head')[0];
            if (head) {
                var styleId = 'osmium-reader-style';
                var style = document.getElementById(styleId);
                if (!style) {
                    style = document.createElement('style');
                    style.id = styleId;
                    head.appendChild(style);
                }
                style.innerHTML = css;
            } else {
                setTimeout(injectStyle, 50);
            }
        }
        injectStyle();

        // 事件委托：挂在 document 捕获阶段，无论图片何时出现都能响应
        // 使用 document 属性标记防止重复注册，确保在 Readium 动态切换内容时也能正常工作
        if (!document.__osmiumClickRegistered) {
            document.addEventListener('click', function(event) {
                var target = event.target;
                if (target.tagName.toUpperCase() === 'IMG' && target.src) {
                    event.preventDefault();
                    event.stopPropagation();
                    if (window.OsmiumBridge) {
                        window.OsmiumBridge.onImageClicked(target.src);
                    }
                }
            }, true);
            document.__osmiumClickRegistered = true;
        }
    })();
    """.trimIndent()
    }

    return remember(script, isOldAndroid) {
        ReaderPlatformSettings(
            injectScript = script,
            atDocumentStart = false,
            needsManualBackgroundTransparency = false,
            needsManualNavigationSync = false
        )
    }
}

