<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="eie-wide"; section>
    <#if section = "prebody">
        <div class="eie-steps">
            <div class="eie-step" data-step-indicator="1" data-state="current">
                <span class="eie-step-circle">
                    <span class="eie-step-num">1</span>
                    <svg class="eie-step-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 6 9 17l-5-5"/>
                    </svg>
                </span>
                <span>About you</span>
            </div>
            <div class="eie-step-divider"></div>
            <div class="eie-step" data-step-indicator="2" data-state="upcoming">
                <span class="eie-step-circle">
                    <span class="eie-step-num">2</span>
                    <svg class="eie-step-check" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 6 9 17l-5-5"/>
                    </svg>
                </span>
                <span>Your interests</span>
            </div>
        </div>
    <#elseif section = "header">
        <span class="eie-wizard-title" data-title-step="1">Tell us about yourself</span>
        <span class="eie-wizard-title" data-title-step="2" hidden>What will you explore?</span>
    <#elseif section = "subheader">
        <p class="eie-subtitle eie-wizard-title" data-title-step="1">This helps EIE tailor how it explains data and what it surfaces first.</p>
    <#elseif section = "form">
        <form id="kc-register-form" action="${url.registrationAction}" method="post">

            <div class="eie-wizard-step" data-step="1" data-active="true">
                <div class="eie-field-row">
                    <div class="eie-field">
                        <label for="firstName">${msg("firstName")} <span class="eie-required">*</span></label>
                        <input type="text" id="firstName" name="firstName" required
                               value="${(register.formData.firstName!'')}" autocomplete="given-name"
                               placeholder="First name"
                               aria-invalid="<#if messagesPerField.existsError('firstName')>true</#if>"
                        />
                        <#if messagesPerField.existsError('firstName')>
                            <span class="input-error" aria-live="polite">${kcSanitize(messagesPerField.get('firstName'))?no_esc}</span>
                        </#if>
                    </div>

                    <div class="eie-field">
                        <label for="lastName">${msg("lastName")} <span class="eie-required">*</span></label>
                        <input type="text" id="lastName" name="lastName" required
                               value="${(register.formData.lastName!'')}" autocomplete="family-name"
                               placeholder="Last name"
                               aria-invalid="<#if messagesPerField.existsError('lastName')>true</#if>"
                        />
                        <#if messagesPerField.existsError('lastName')>
                            <span class="input-error" aria-live="polite">${kcSanitize(messagesPerField.get('lastName'))?no_esc}</span>
                        </#if>
                    </div>
                </div>

                <div class="eie-field">
                    <label for="email">Email address</label>
                    <input type="email" id="email" name="email" required class="eie-input-muted"
                           value="${(register.formData.email!'')}" autocomplete="email"
                           placeholder="you@nasa.gov"
                           aria-invalid="<#if messagesPerField.existsError('email')>true</#if>"
                    />
                    <#if messagesPerField.existsError('email')>
                        <span class="input-error" aria-live="polite">${kcSanitize(messagesPerField.get('email'))?no_esc}</span>
                    </#if>
                </div>

                <div class="eie-field-row">
                    <div class="eie-field">
                        <label for="organization">Organization <span class="eie-required">*</span></label>
                        <input type="text" id="organization" name="organization" required
                               value="${(register.formData.organization!'')}"
                               placeholder="e.g. NASA GSFC, NOAA, USGS, WRI"
                               aria-invalid="<#if messagesPerField.existsError('organization')>true</#if>"
                        />
                        <#if messagesPerField.existsError('organization')>
                            <span class="input-error" aria-live="polite">${kcSanitize(messagesPerField.get('organization'))?no_esc}</span>
                        </#if>
                    </div>

                    <div class="eie-field">
                        <label for="jobTitle">Job title <span class="eie-optional">(optional)</span></label>
                        <input type="text" id="jobTitle" name="jobTitle"
                               value="${(register.formData.jobTitle!'')}"
                               placeholder="e.g. Research Scientist"
                        />
                    </div>
                </div>

                <div class="eie-field-row">
                    <div class="eie-field">
                        <label for="role">Role <span class="eie-required">*</span></label>
                        <span class="eie-field-hint">How you primarily work with Earth data</span>
                        <select id="role" name="role" required>
                            <option value="" <#if !(register.formData.role)?has_content>selected</#if> disabled>Select a role</option>
                            <option value="researcher" <#if (register.formData.role!'') == 'researcher'>selected</#if>>Research scientist</option>
                            <option value="policy" <#if (register.formData.role!'') == 'policy'>selected</#if>>Policy / reporting</option>
                            <option value="educator" <#if (register.formData.role!'') == 'educator'>selected</#if>>Educator</option>
                            <option value="developer" <#if (register.formData.role!'') == 'developer'>selected</#if>>Developer / tool builder</option>
                            <option value="communicator" <#if (register.formData.role!'') == 'communicator'>selected</#if>>Science communicator</option>
                            <option value="other" <#if (register.formData.role!'') == 'other'>selected</#if>>Other</option>
                        </select>
                    </div>

                    <div class="eie-field">
                        <label for="country">Country <span class="eie-optional">(optional)</span></label>
                        <span class="eie-field-hint" aria-hidden="true">&nbsp;</span>
                        <select id="country" name="country">
                            <option value="" selected>Select country</option>
                            <option value="US">United States</option>
                            <option value="CA">Canada</option>
                            <option value="MX">Mexico</option>
                            <option value="GB">United Kingdom</option>
                            <option value="DE">Germany</option>
                            <option value="FR">France</option>
                            <option value="IN">India</option>
                            <option value="BR">Brazil</option>
                            <option value="AU">Australia</option>
                            <option value="other">Other</option>
                        </select>
                    </div>
                </div>

                <div class="eie-field">
                    <label>Data familiarity <span class="eie-required">*</span></label>
                    <span class="eie-field-hint">EIE uses this to calibrate how it explains results - no wrong answer.</span>
                    <div class="eie-card-group" role="radiogroup" aria-label="Data familiarity">
                        <label class="eie-card-choice">
                            <input type="radio" name="dataFamiliarity" value="explorer" <#if (register.formData.dataFamiliarity!'') == 'explorer'>checked</#if> required />
                            <span class="eie-card-body">
                                <span class="eie-card-title">Curious explorer</span>
                                <span class="eie-card-desc">New to Earth data, using EIE to learn and discover</span>
                            </span>
                        </label>
                        <label class="eie-card-choice">
                            <input type="radio" name="dataFamiliarity" value="practitioner" <#if (register.formData.dataFamiliarity!'') == 'practitioner'>checked</#if> />
                            <span class="eie-card-body">
                                <span class="eie-card-title">Data practitioner</span>
                                <span class="eie-card-desc">Familiar with datasets, charts, and basic spatial concepts</span>
                            </span>
                        </label>
                        <label class="eie-card-choice">
                            <input type="radio" name="dataFamiliarity" value="gis" <#if (register.formData.dataFamiliarity!'') == 'gis'>checked</#if> />
                            <span class="eie-card-body">
                                <span class="eie-card-title">GIS / Remote sensing</span>
                                <span class="eie-card-desc">Experienced with satellite data, geospatial analysis, or modeling</span>
                            </span>
                        </label>
                    </div>
                </div>

                <div class="eie-field-row">
                    <div class="eie-field">
                        <label for="password">Password <span class="eie-required">*</span></label>
                        <input type="password" id="password" name="password" required
                               autocomplete="new-password"
                               placeholder="Create a password"
                               aria-invalid="<#if messagesPerField.existsError('password','password-confirm')>true</#if>"
                        />
                        <#if messagesPerField.existsError('password')>
                            <span class="input-error" aria-live="polite">${kcSanitize(messagesPerField.get('password'))?no_esc}</span>
                        </#if>
                    </div>

                    <div class="eie-field">
                        <label for="password-confirm">Confirm password <span class="eie-required">*</span></label>
                        <input type="password" id="password-confirm" name="password-confirm" required
                               autocomplete="new-password"
                               placeholder="Re-enter password"
                               aria-invalid="<#if messagesPerField.existsError('password-confirm')>true</#if>"
                        />
                        <#if messagesPerField.existsError('password-confirm')>
                            <span class="input-error" aria-live="polite">${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}</span>
                        </#if>
                    </div>
                </div>

                <div class="eie-row-actions">
                    <a href="${url.loginUrl}" class="eie-btn-secondary">&larr; Back to sign in</a>
                    <button type="button" class="eie-btn-primary" data-wizard-next="2">Continue &rarr;</button>
                </div>
            </div>

            <div class="eie-wizard-step" data-step="2">
                <div class="eie-field">
                    <label>Earth science topics <span class="eie-optional">(choose all that apply)</span></label>
                    <span class="eie-field-hint">These become your default dataset suggestions in the chat.</span>
                    <div class="eie-chip-group" role="group" aria-label="Earth science topics">
                        <#list ["Climate & temperature","Wildfires","Sea ice & polar regions","Oceans & coastlines","Land use & agriculture","Air quality & atmosphere","Vegetation & ecosystems","Water & drought","Terrain & geology","Urban & population"] as topic>
                            <label class="eie-chip">
                                <input type="checkbox" name="topics" value="${topic}" <#if topic == "Climate & temperature">checked</#if> />
                                <span>${topic}</span>
                            </label>
                        </#list>
                    </div>
                </div>

                <div class="eie-field">
                    <label>Primary geographic region <span class="eie-optional">(optional)</span></label>
                    <span class="eie-field-hint">Sets your default map view. You can always zoom or search elsewhere.</span>
                    <div class="eie-chip-group" role="radiogroup" aria-label="Primary geographic region">
                        <#list ["Global","North America","Latin America","Europe & Africa","Asia & Pacific","Polar regions"] as region>
                            <label class="eie-chip">
                                <input type="radio" name="region" value="${region}" <#if region == "Global">checked</#if> />
                                <span>${region}</span>
                            </label>
                        </#list>
                    </div>
                </div>

                <div class="eie-field">
                    <label>How do you plan to use EIE? <span class="eie-optional">(optional)</span></label>
                    <div class="eie-chip-group" role="group" aria-label="How do you plan to use EIE">
                        <#list ["Exploring & learning","Research & analysis","Policy & reporting","Science communication","Education & teaching","Tool development / testing"] as useCase>
                            <label class="eie-chip">
                                <input type="checkbox" name="useCase" value="${useCase}" <#if useCase == "Exploring & learning">checked</#if> />
                                <span>${useCase}</span>
                            </label>
                        </#list>
                    </div>
                </div>

                <hr class="eie-divider" />

                <label class="eie-check-row">
                    <input type="checkbox" name="updatesOptIn" value="true" />
                    <span>
                        <span class="eie-check-label">Send me updates about EIE new datasets, features, and Earth science insights.</span>
                        <span class="eie-check-hint">A few emails per month - unsubscribe anytime.</span>
                    </span>
                </label>

                <label class="eie-check-row">
                    <input type="checkbox" name="contactOptIn" value="true" checked />
                    <span class="eie-check-label">Contact me about testing new features and participating in user research.</span>
                </label>

                <#if recaptchaRequired??>
                    <div class="eie-field">
                        <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
                    </div>
                </#if>

                <div class="eie-row-actions">
                    <button type="button" class="eie-btn-secondary" data-wizard-back="1">&larr; Back</button>
                    <button class="eie-btn-primary" type="submit">Start exploring &rarr;</button>
                </div>
            </div>
        </form>

        <script nonce="${stateChecker!''}">
            (function () {
                var form = document.getElementById('kc-register-form');
                if (!form) return;
                var steps = form.querySelectorAll('.eie-wizard-step');
                var indicators = document.querySelectorAll('.eie-step[data-step-indicator]');
                var titles = document.querySelectorAll('[data-title-step]');

                function showStep(target) {
                    steps.forEach(function (step) {
                        step.setAttribute('data-active', step.getAttribute('data-step') === target ? 'true' : 'false');
                    });
                    indicators.forEach(function (ind) {
                        var n = ind.getAttribute('data-step-indicator');
                        if (n === target) {
                            ind.setAttribute('data-state', 'current');
                        } else if (parseInt(n, 10) < parseInt(target, 10)) {
                            ind.setAttribute('data-state', 'done');
                        } else {
                            ind.setAttribute('data-state', 'upcoming');
                        }
                    });
                    titles.forEach(function (t) {
                        t.hidden = t.getAttribute('data-title-step') !== target;
                    });
                    window.scrollTo({ top: 0, behavior: 'instant' in window ? 'instant' : 'auto' });
                }

                form.querySelectorAll('[data-wizard-next]').forEach(function (btn) {
                    btn.addEventListener('click', function () {
                        var currentStep = btn.closest('.eie-wizard-step');
                        var invalidField = currentStep.querySelector(':invalid');
                        if (invalidField) {
                            currentStep.reportValidity ? currentStep.reportValidity() : invalidField.reportValidity();
                            invalidField.focus();
                            return;
                        }
                        showStep(btn.getAttribute('data-wizard-next'));
                    });
                });

                form.querySelectorAll('[data-wizard-back]').forEach(function (btn) {
                    btn.addEventListener('click', function () {
                        showStep(btn.getAttribute('data-wizard-back'));
                    });
                });
            })();
        </script>
    </#if>
</@layout.registrationLayout>
