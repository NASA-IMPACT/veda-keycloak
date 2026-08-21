<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true; section>
    <#if section = "header">
        Forgot your password?
    <#elseif section = "subheader">
        <p class="eie-subtitle">Enter the email address you sign in with and we'll send you a link to reset your password.</p>
    <#elseif section = "form">
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
            <div class="eie-field">
                <label for="username">
                    <#if !realm.loginWithEmailAllowed>${msg("username")}
                    <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                    <#else>Email address
                    </#if>
                </label>
                <input type="text" id="username" name="username" autofocus value="${(auth.attemptedUsername!'')}"
                       placeholder="you@nasa.gov"
                       aria-invalid="<#if messagesPerField.existsError('username')>true</#if>" />
                <#if messagesPerField.existsError('username')>
                    <span class="input-error" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('username'))?no_esc}
                    </span>
                </#if>
            </div>

            <div class="eie-row-actions">
                <a href="${(url.loginRestartFlowUrl!'')?has_content?then(url.loginRestartFlowUrl, url.loginUrl)}" class="eie-btn-secondary">&larr; Back to sign in</a>
                <button class="eie-btn-primary" type="submit">Send reset link &rarr;</button>
            </div>
        </form>
    <#elseif section = "info">
        <#if realm.duplicateEmailsAllowed>
            ${msg("emailInstructionUsername")}
        <#else>
            ${msg("emailInstruction")}
        </#if>
    </#if>
</@layout.registrationLayout>
