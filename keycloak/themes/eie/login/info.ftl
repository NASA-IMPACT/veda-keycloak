<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        <#if messageHeader??>
            ${messageHeader}
        <#else>
            ${message.summary}
        </#if>
    <#elseif section = "subheader">
    <#elseif section = "form">
        <div class="eie-status-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 4h16v16H4z" opacity="0"/>
                <path d="M22 6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6zm-2 0-8 5-8-5h16zm0 12H4V8l8 5 8-5v10z"/>
            </svg>
        </div>

        <#if !messageHeader??>
            <#-- The header already shows the summary. On the "Email verified"
                 page, use the body to say what happens next instead. -->
            <#if message.summary == msg("emailVerified")>
                <p class="eie-subtitle">${msg("eieAccountUnderReview")}</p>
            <#else>
                <p class="eie-subtitle">${message.summary}</p>
            </#if>
        </#if>

        <#if requiredActions??>
            <p class="eie-subtitle">
                <#list requiredActions>You need to complete the following actions:<ul><#items as reqActionItem><li>${msg("requiredAction.${reqActionItem}")}</li></#items></ul></#list>
            </p>
        </#if>

        <#if skipLink??>
        <#else>
            <#if pageRedirectUri?has_content>
                <a href="${pageRedirectUri}" class="eie-btn-primary" style="display:inline-flex;width:auto;padding:0.6rem 1.5rem;">${kcSanitize(msg("backToApplication"))?no_esc}</a>
            <#elseif actionUri?has_content>
                <a href="${actionUri}" class="eie-btn-primary" style="display:inline-flex;width:auto;padding:0.6rem 1.5rem;">${kcSanitize(msg("proceedWithAction"))?no_esc}</a>
            <#elseif (client.baseUrl)?has_content>
                <a href="${client.baseUrl}" class="eie-btn-primary" style="display:inline-flex;width:auto;padding:0.6rem 1.5rem;">${kcSanitize(msg("backToApplication"))?no_esc}</a>
            </#if>
        </#if>

        <div class="eie-footer">
            <a href="${url.loginUrl}">&larr; Back to sign in</a>
        </div>
    </#if>
</@layout.registrationLayout>
