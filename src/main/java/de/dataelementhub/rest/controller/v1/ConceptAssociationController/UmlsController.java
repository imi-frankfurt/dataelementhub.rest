package de.dataelementhub.rest.controller.v1.ConceptAssociationController;
import static de.dataelementhub.rest.controller.v1.ApiVersion.API_VERSION;
import de.dataelementhub.model.service.Terminologies.UmlsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(API_VERSION + "/umls")
public class UmlsController {

    private final UmlsService umlsService;

    public UmlsController(UmlsService umlsService) {
        this.umlsService = umlsService;
    }

// Search in all Ontologies
    @GetMapping("/search")
    public List<UmlsService.UmlsConcept> search(@RequestParam String term) throws IOException {
        return umlsService.searchTerm(term);
    }
    // Search in one specific ontology
    @GetMapping("/searchInOntology")
    public List<UmlsService.UmlsConcept> searchConcepts(
            @RequestParam("query") String query,
            @RequestParam("sabs")  String sabs) throws IOException {

        String q = query == null ? null : query.trim();
        String s = sabs  == null ? null : sabs.trim();
        if (q == null || q.isEmpty() || s == null || s.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or empty 'query' or 'sabs'");
        }
        return umlsService.searchTermInOntology(q, s);
    }
    @GetMapping("/ids")
    public List<String> getOntologyIds() {
        return umlsService.getAllOntologyIds();
    }
}