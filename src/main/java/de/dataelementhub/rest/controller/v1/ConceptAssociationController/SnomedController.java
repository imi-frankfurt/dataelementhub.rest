package de.dataelementhub.rest.controller.v1.ConceptAssociationController;

import de.dataelementhub.model.service.Terminologies.SnomedService;
import de.dataelementhub.model.service.Terminologies.SnomedService.SnomedConcept;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/snomed")
public class SnomedController {

    private final SnomedService snomedService;

    @Autowired
    public SnomedController(SnomedService snomedService) {
        this.snomedService = snomedService;
    }

    /**
     * Endpoint to search SNOMED concepts by term.
     * Example: GET /v1/snomed/search?term=diabetes&offset=0&limit=10
     *
     * @param term   The search term (e.g., "diabetes")
     * @param offset Pagination offset
     * @param limit  Number of results to return
     * @return A list of SnomedConcept objects
     */
    @GetMapping("/search")
    public List<SnomedConcept> searchByTerm(
            @RequestParam String term,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {

        return snomedService.searchByTerm(term, offset, limit);
    }
}