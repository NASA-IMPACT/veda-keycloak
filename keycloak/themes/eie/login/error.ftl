<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("errorTitle")}
    <#elseif section = "subheader">
        <p class="eie-subtitle">${msg("errorTitleHtml")?no_esc?default("")}</p>
    <#elseif section = "form">
        <div class="alert alert-error" role="alert">
            <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
        </div>

        <#if skipLink??>
        <#else>
            <#if client?? && client.baseUrl?has_content>
                <div class="eie-footer" style="margin-top:1rem;">
                    <a id="backToApplication" href="${client.baseUrl}">${kcSanitize(msg("backToApplication"))?no_esc}</a>
                </div>
            <#else>
                <div class="eie-footer" style="margin-top:1rem;">
                    <a href="${(url.loginRestartFlowUrl!'')?has_content?then(url.loginRestartFlowUrl, url.loginUrl)}">&larr; Back to sign in</a>
                </div>
            </#if>
        </#if>
    </#if>
</@layout.registrationLayout>
