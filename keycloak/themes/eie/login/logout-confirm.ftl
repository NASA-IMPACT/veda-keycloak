<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="eie-center" displayMessage=false; section>
    <#if section = "header">
        ${msg("eieSigningOut")}
    <#elseif section = "form">
        <p class="eie-subtitle">${msg("eieLogoutFallback")}</p>
        <form id="eie-logout-form" action="${url.logoutConfirmAction}" method="POST">
            <input type="hidden" name="session_code" value="${logoutConfirm.code}">
            <button class="eie-btn-primary" name="confirmLogout" id="kc-logout" type="submit">
                ${msg("doLogout")}
            </button>
        </form>
        <script src="${url.resourcesPath}/js/logout.js" defer></script>
    </#if>
</@layout.registrationLayout>
