package br.com.rizermarketplaces.core.marketplace.controller.publicapi;

import br.com.rizermarketplaces.core.marketplace.integration.GoogleShoppingFeedService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/{countryCode}/public/tenants")
@Tag(name = "Público · Feeds", description = "Feeds de produto para Google Merchant Center e Meta Commerce Manager")
public class PublicFeedsController {

    private final GoogleShoppingFeedService googleFeed;

    public PublicFeedsController(GoogleShoppingFeedService googleFeed) {
        this.googleFeed = googleFeed;
    }

    @GetMapping(value = "/{slug}/feed.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> googleShopping(@PathVariable String slug) {
        String xml = googleFeed.renderFeed(slug);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .header("Content-Disposition", "inline; filename=\"" + slug + "-gmc.xml\"")
            .body(xml);
    }

    /**
     * Feed alternativo para Meta Commerce Manager (formato CSV,
     * mais simples de manter do que XML). Em produção, prefira
     * Graph API (criado por MetaCatalogService.syncTenantProducts).
     */
    @GetMapping(value = "/{slug}/feed-meta.csv", produces = "text/csv")
    public ResponseEntity<String> metaCatalogCsv(@PathVariable String slug) {
        // TODO(fase-6-meta-feed): gerar CSV idêntico ao feed Google mas
        // com cabeçalhos exigidos pelo Meta Catalog (id,title,description,
        // availability,condition,price,link,image_link,brand).
        return ResponseEntity.ok().body("id,title,description,availability,condition,price,link,image_link,brand\n");
    }
}
