package de.dataelementhub.rest.controller.v1.ConceptAssociationController;

import de.dataelementhub.model.service.Terminologies.SemlookpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static de.dataelementhub.rest.controller.v1.ApiVersion.API_VERSION;

@RestController
@RequestMapping(API_VERSION + "/semlookp")
public class SemlookpController {
    private final SemlookpService semlookpService;

    public SemlookpController(SemlookpService lookupService) {
        this.semlookpService = lookupService;
    }

    // Search in a specific ontology
    @GetMapping("/search")
    public List<SemlookpService.Concept> searchConcepts(@RequestParam String query, String ontology) {
        return semlookpService.searchInOntology(query, ontology);
    }
    //search in all Ontologies
    @GetMapping("/searchInAllOntology")
    public List<SemlookpService.Concept> searchConcepts(
            @RequestParam("query") String query) throws IOException {
        return semlookpService.searchAllOntologies(query);
    }
    @GetMapping("/ids")
    public List<String> getOntologyIds() {
        return semlookpService.getAllOntologyIds();
    }
}
