package com.newproject.blog.service;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * SECURITY (H7): sanitizzazione del content HTML dei blog post prima della persistenza.
 *
 * <p>Il web-portal renderizza il content con {@code th:utext} (HTML non escaped): senza
 * sanitizzazione un {@code <script>} o un handler {@code onerror=} salvato da un admin
 * (o da un admin compromesso) diventerebbe stored-XSS su tutti i visitatori del blog.
 * La policy e' allineata a quella del CMS ({@code CmsHtmlSanitizer}): consente formattazione,
 * blocchi, link, immagini, tabelle e stili inline, ma elimina {@code <script>}, gli attributi
 * {@code on*} e i protocolli pericolosi ({@code javascript:}).
 */
public final class BlogHtmlSanitizer {

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)        // href solo http/https/mailto, rel=nofollow
            .and(Sanitizers.IMAGES)       // src solo http/https, niente javascript:
            .and(Sanitizers.TABLES)
            .and(Sanitizers.STYLES)
            .and(new HtmlPolicyBuilder()
                    .allowElements("h1", "h2", "h3", "h4", "h5", "h6", "hr", "section", "article")
                    .toFactory());

    private BlogHtmlSanitizer() {}

    /** Restituisce l'HTML ripulito; {@code null} resta {@code null}. */
    public static String sanitize(String html) {
        if (html == null) return null;
        return POLICY.sanitize(html);
    }
}
