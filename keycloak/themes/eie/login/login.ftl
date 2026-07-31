<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password') displayInfo=false; section>
    <#if section = "header">
        Sign in to <span class="eie-brand">EIE</span>
    <#elseif section = "subheader">
        <p class="eie-subtitle">Earth Information Explorer is currently in internal preview. Sign in with your NASA or approved organization account.</p>
    <#elseif section = "form">
        <#if realm.registrationAllowed>
            <div class="eie-notice">
                <span>
                    Access is currently limited to NASA employees and invited collaborators.
                    <a href="${url.registrationUrl}">Request access &rarr;</a>
                </span>
            </div>
        </#if>

        <div id="kc-form">
            <div id="kc-form-wrapper">
                <#if realm.password>
                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                        <#if !usernameHidden??>
                            <div class="eie-field">
                                <label for="username">
                                    <#if !realm.loginWithEmailAllowed>${msg("username")}
                                    <#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}
                                    <#else>Email address
                                    </#if>
                                    <span class="eie-required">*</span>
                                </label>
                                <input tabindex="1" id="username" name="username" value="${(login.username!'')}" type="text" autofocus autocomplete="off"
                                       placeholder="you@nasa.gov"
                                       aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>" />
                                <#if messagesPerField.existsError('username','password')>
                                    <span class="input-error" aria-live="polite">
                                        ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                                    </span>
                                </#if>
                            </div>
                        </#if>

                        <div class="eie-field">
                            <label for="password">Password <span class="eie-required">*</span></label>
                            <div class="eie-password-wrap">
                                <input tabindex="2" id="password" name="password" type="password" autocomplete="off"
                                       placeholder="Enter password"
                                       aria-invalid="<#if messagesPerField.existsError('username','password')>true</#if>" />
                                <button type="button" class="eie-password-toggle"
                                        data-password-input="password"
                                        data-label-show="Show"
                                        data-label-hide="Hide"
                                        aria-label="Show password">
                                    Show
                                </button>
                            </div>
                            <#if usernameHidden?? && messagesPerField.existsError('username','password')>
                                <span class="input-error" aria-live="polite">
                                    ${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}
                                </span>
                            </#if>
                        </div>

                        <#if realm.resetPasswordAllowed>
                            <div class="eie-row" style="justify-content:flex-end;">
                                <a tabindex="5" href="${url.loginResetCredentialsUrl}">Forgot password?</a>
                            </div>
                        </#if>

                        <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>

                        <div class="eie-row-actions">
                            <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
                                <a href="${url.registrationUrl}">Create an account</a>
                            <#else>
                                <span></span>
                            </#if>
                            <button tabindex="4" class="eie-btn-primary" name="login" id="kc-login" type="submit">Sign in &rarr;</button>
                        </div>
                    </form>
                </#if>
            </div>
        </div>
        <script nonce="${stateChecker!''}">
            (function () {
                var btn = document.querySelector('.eie-password-toggle');
                if (!btn) return;
                btn.addEventListener('click', function () {
                    var input = document.getElementById(btn.getAttribute('data-password-input'));
                    if (!input) return;
                    var isPassword = input.type === 'password';
                    input.type = isPassword ? 'text' : 'password';
                    var label = isPassword ? btn.getAttribute('data-label-hide') : btn.getAttribute('data-label-show');
                    btn.textContent = label;
                    btn.setAttribute('aria-label', label);
                });
            })();
        </script>

        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div class="eie-footer">
                <strong>Don't have access yet?</strong><br />
                <a tabindex="6" href="${url.registrationUrl}">Request access to the internal preview</a>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
