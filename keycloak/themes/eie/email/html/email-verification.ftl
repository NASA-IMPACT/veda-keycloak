<#--
  Verification email, sent at registration (realm has verifyEmail=true).
  Inline styles and tables on purpose: email clients handle <style> poorly.
-->
<html>
<body style="margin:0;padding:0;background-color:#f0f0f0;">
  <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;">
    Confirm your email address to continue your Earth Information Explorer access request.
  </div>
  <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="background-color:#f0f0f0;padding:32px 16px;">
    <tr>
      <td align="center">
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="max-width:560px;background-color:#ffffff;border:1px solid #dfe1e2;border-radius:4px;color:#1c1d1f;">
          <tr>
            <td style="padding:30px 32px 0 32px;font-family:'Public Sans','Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;">
              <p style="margin:0 0 22px 0;font-size:13px;font-weight:700;line-height:1.2;color:#0050d8;">Earth Information Explorer</p>
              <h1 style="margin:0 0 14px 0;font-size:24px;line-height:1.3;font-weight:700;color:#1c1d1f;">Confirm your email address</h1>
              <p style="margin:0 0 14px 0;font-size:16px;line-height:1.55;color:#454749;">
                <#if user.firstName??>Hi ${user.firstName},<#else>Hello,</#if>
              </p>
              <p style="margin:0 0 22px 0;font-size:16px;line-height:1.55;color:#454749;">
                Thanks for requesting access to Earth Information Explorer. Confirm this email address to continue.
              </p>
            </td>
          </tr>
          <tr>
            <td style="padding:0 32px 24px 32px;">
              <a href="${link}" style="display:inline-block;padding:11px 20px;background-color:#0050d8;border:1px solid #0050d8;color:#ffffff;font-family:'Public Sans','Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;font-size:15px;line-height:1.35;font-weight:700;text-decoration:none;border-radius:4px;">Confirm email address</a>
            </td>
          </tr>
          <tr>
            <td style="padding:0 32px 32px 32px;font-family:'Public Sans','Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif;">
              <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="margin:0 0 20px 0;background-color:#f7f7f7;border:1px solid #dfe1e2;border-radius:4px;">
                <tr>
                  <td style="padding:14px 16px;">
                    <p style="margin:0 0 6px 0;font-size:13px;line-height:1.45;font-weight:700;color:#1c1d1f;">What happens next</p>
                    <p style="margin:0;font-size:14px;line-height:1.55;color:#454749;">
                      After that, an Earth Information Explorer administrator reviews your request, usually within two business days. We will email you as soon as your account is approved.
                    </p>
                  </td>
                </tr>
              </table>
              <p style="margin:0 0 14px 0;font-size:14px;line-height:1.55;color:#71767a;">
                This link expires in ${linkExpirationFormatter(linkExpiration)}.
              </p>
              <p style="margin:0;padding-top:16px;border-top:1px solid #dfe1e2;font-size:14px;line-height:1.55;color:#71767a;">
                If you did not request an Earth Information Explorer account, you can ignore this message.
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
