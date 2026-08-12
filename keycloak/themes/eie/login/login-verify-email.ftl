<#--
  Shown while the VERIFY_EMAIL required action is pending, i.e. straight after
  registration. At this point the account is neither confirmed nor approved, so
  this page only tells the user what to do next: confirm the address, then wait
  for the approval email sent by the email-on-approval listener.
-->
<#import "template.ftl" as layout>
<#-- displayMessage=false: Keycloak's stock "you need to verify your email"
     warning is redundant here, and it re-renders unchanged after a re-send. -->
<@layout.registrationLayout bodyClass="eie-center" displayMessage=false displayInfo=true; section>
    <#if section = "header">
        Check your email
    <#elseif section = "subheader">
    <#elseif section = "form">
        <div class="eie-status-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6zm-2 0-8 5-8-5h16zm0 12H4V8l8 5 8-5v10z"/>
            </svg>
        </div>

        <p class="eie-subtitle">
            <#if user?? && user.email?has_content>
                We sent a confirmation link to <strong>${user.email}</strong>. Open it to confirm your email address.
            <#else>
                We sent a confirmation link to your email address. Open it to confirm the address.
            </#if>
        </p>

        <p class="eie-subtitle">
            After that, an Earth Information Explorer administrator reviews your request, usually within two business days. We will email you as soon as your account is approved.
        </p>
    <#elseif section = "info">
        Did not get the email? <a href="${url.loginAction}">Send it again</a>.
    </#if>
</@layout.registrationLayout>
