package de.dataelementhub.rest.controller.v1.ConceptAssociationController;

import de.dataelementhub.model.service.Terminologies.SemlookpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static de.dataelementhub.rest.controller.v1.ApiVersion.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/semlookp")
public class SemlookpController {
    private final SemlookpService lookupService;

    public SemlookpController(SemlookpService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/search")
    public List<SemlookpService.Concept> searchConcepts(@RequestParam String query, String ontology) {
        return lookupService.searchInOntology(query, ontology);
    }
    @GetMapping("/ids")
    public List<String> getOntologyIds() {
        return lookupService.getAllOntologyIds();
    }
}
