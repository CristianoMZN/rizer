package br.com.rizermarketplaces.core.marketplace.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

/**
 * Resolve CNAME e A records via JNDI (puro Java).
 * Em prod, prefira um cliente DNS-over-HTTPS (Cloudflare 1.1.1.1 ou Google 8.8.8.8)
 * para evitar manipulação de DNS local. Aqui mantemos JNDI porque não temos
 * DNS-over-HTTPS configurado e é zero-dependência.
 */
@Service
public class DnsLookupService {

    private static final Logger log = LoggerFactory.getLogger(DnsLookupService.class);

    public static class DnsResult {
        public final String cnameTarget;     // pode ser null se não houver CNAME
        public final String resolvedIp;      // IP do CNAME (se houver) ou do A
        public final String error;

        public DnsResult(String cnameTarget, String resolvedIp, String error) {
            this.cnameTarget = cnameTarget;
            this.resolvedIp = resolvedIp;
            this.error = error;
        }

        public boolean ok() { return error == null; }
    }

    public DnsResult lookup(String domain) {
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns://1.1.1.1");
            DirContext ctx = new InitialDirContext(env);

            // 1) CNAME
            String cname = queryCname(ctx, domain);
            // 2) A do CNAME (se houver) ou do próprio domínio
            String host = cname != null ? cname : domain;
            String ip = queryA(ctx, host);
            if (ip == null) {
                return new DnsResult(cname, null, "Nenhum CNAME ou A record encontrado para " + host);
            }
            return new DnsResult(cname, ip, null);
        } catch (Exception e) {
            log.warn("[dns] lookup falhou para {}: {}", domain, e.getMessage());
            return new DnsResult(null, null, "Falha DNS: " + e.getMessage());
        }
    }

    private String queryCname(DirContext ctx, String domain) throws Exception {
        Attributes attrs = ctx.getAttributes(domain, new String[]{"CNAME"});
        Attribute cname = attrs.get("CNAME");
        if (cname == null) return null;
        String value = cname.get().toString();
        // remove trailing dot
        return value.endsWith(".") ? value.substring(0, value.length() - 1) : value;
    }

    private String queryA(DirContext ctx, String domain) throws Exception {
        Attributes attrs = ctx.getAttributes(domain, new String[]{"A"});
        Attribute a = attrs.get("A");
        if (a == null) return null;
        return a.get().toString();
    }

}
