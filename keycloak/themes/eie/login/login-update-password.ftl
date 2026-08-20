<#import "template.ftl" as layout>
<#import "password-commons.ftl" as passwordCommons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('password','password-confirm'); section>
    <#if section = "header">
        Update your password
    <#elseif section = "subheader">
        <p class="eie-subtitle">Create a new password for your Earth Information Explorer account.</p>
    <#elseif section = "form">
        <form id="kc-passwd-update-form" action="${url.loginAction}" method="post">
            <div class="eie-field">
                <label for="password-new">New password <span class="eie-required">*</span></label>
                <div class="eie-password-wrap">
                    <input type="password" id="password-new" name="password-new" required
                           autofocus autocomplete="new-password"
                           placeholder="Create a new password"
                           aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
                    />
                    <button type="button" class="eie-password-toggle"
                            data-password-input="password-new"
                            data-label-show="Show"
                            data-label-hide="Hide"
                            aria-label="Show password">
                        Show
                    </button>
                </div>
                <#if messagesPerField.existsError('password')>
                    <span id="input-error-password" class="input-error" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('password'))?no_esc}
                    </span>
                </#if>
            </div>

            <div class="eie-field">
                <label for="password-confirm">Confirm password <span class="eie-required">*</span></label>
                <div class="eie-password-wrap">
                    <input type="password" id="password-confirm" name="password-confirm" required
                           autocomplete="new-password"
                           placeholder="Re-enter new password"
                           aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
                    />
                    <button type="button" class="eie-password-toggle"
                            data-password-input="password-confirm"
                            data-label-show="Show"
                            data-label-hide="Hide"
                            aria-label="Show password">
                        Show
                    </button>
                </div>
                <#if messagesPerField.existsError('password-confirm')>
                    <span id="input-error-password-confirm" class="input-error" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                    </span>
                </#if>
            </div>

            <#if isAppInitiatedAction??>
                <label class="eie-check-row">
                    <input type="checkbox" id="logout-sessions" name="logout-sessions" value="on" checked />
                    <span class="eie-check-label"><@passwordCommons.logoutOtherSessions/></span>
                </label>

                <div class="eie-row-actions">
                    <button class="eie-btn-secondary" type="submit" name="cancel-aia" value="true">${msg("doCancel")}</button>
                    <button class="eie-btn-primary" type="submit">Update password &rarr;</button>
                </div>
            <#else>
                <button class="eie-btn-primary" type="submit">Update password &rarr;</button>
            </#if>
        </form>

        <script nonce="${stateChecker!''}">
            (function () {
                document.querySelectorAll('.eie-password-toggle').forEach(function (btn) {
                    btn.addEventListener('click', function () {
                        var input = document.getElementById(btn.getAttribute('data-password-input'));
                        if (!input) return;
                        var isPassword = input.type === 'password';
                        input.type = isPassword ? 'text' : 'password';
                        var label = isPassword ? btn.getAttribute('data-label-hide') : btn.getAttribute('data-label-show');
                        btn.textContent = label;
                        btn.setAttribute('aria-label', label);
                    });
                });
            })();
        </script>
    </#if>
</@layout.registrationLayout>
