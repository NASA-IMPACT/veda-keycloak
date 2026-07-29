<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="eie-html" lang="${(locale.currentLanguageTag)!'en'}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="robots" content="noindex, nofollow">
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/NASALogo.svg">
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
</head>
<body class="eie-body ${bodyClass}">
    <div class="eie-shell">
        <div class="eie-panel">
            <#nested "prebody">

            <h1 class="eie-title"><#nested "header"></h1>

            <#nested "subheader">

            <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                <div class="alert alert-${message.type}">
                    <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                </div>
            </#if>

            <#nested "form">

            <#if auth?has_content && auth.showTryAnotherWayLink()>
                <form action="${url.loginAction}" method="post">
                    <input type="hidden" name="tryAnotherWay" value="on"/>
                    <div class="eie-footer">
                        <a href="#" onclick="document.forms['kc-select-try-another-way-form'].submit();return false;">${msg("doTryAnotherWay")}</a>
                    </div>
                </form>
            </#if>

            <#if displayInfo>
                <div class="eie-footer">
                    <#nested "info">
                </div>
            </#if>
        </div>
    </div>
    <script nonce="${stateChecker!''}">
        (function () {
            document.querySelectorAll('.eie-field').forEach(function (field) {
                var errorEl = field.querySelector('.input-error');
                if (!errorEl) return;
                field.querySelectorAll('input, select, textarea').forEach(function (control) {
                    control.addEventListener('input', function () {
                        errorEl.style.display = 'none';
                        control.setAttribute('aria-invalid', 'false');
                    });
                });
            });

            var pageAlert = document.querySelector('.alert-error');
            if (pageAlert) {
                document.querySelectorAll('form input, form select, form textarea').forEach(function (control) {
                    control.addEventListener('input', function () {
                        pageAlert.style.display = 'none';
                    }, { once: true });
                });
            }
        })();
    </script>
</body>
</html>
</#macro>
