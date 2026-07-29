<#import "template.ftl" as layout>
<@layout.registrationLayout bodyClass="eie-center" displayMessage=false; section>
    <#if section = "header">
        You&#39;re all set
    <#elseif section = "subheader">
    <#elseif section = "form">
        <div class="eie-status-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M20 6 9 17l-5-5"/>
            </svg>
        </div>

        <p class="eie-subtitle" style="text-align:center;">
            We&#39;ve loaded a default layer to get you started. Ask EIE to find the right dataset for what you&#39;re looking at, and it&#39;ll surface quick summary stats.
        </p>

        <#if client?? && client.baseUrl?has_content>
            <a href="${client.baseUrl}" class="eie-btn-primary">Open Earth Information Explorer &rarr;</a>
        <#else>
            <a href="${url.loginUrl}" class="eie-btn-primary">Open Earth Information Explorer &rarr;</a>
        </#if>

        <p class="eie-footer">You have <strong>20 free queries</strong> in the internal preview. Usage resets monthly.</p>
    </#if>
</@layout.registrationLayout>
