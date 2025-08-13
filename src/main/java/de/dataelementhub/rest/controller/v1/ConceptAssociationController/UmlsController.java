package de.dataelementhub.rest.controller.v1.ConceptAssociationController;
import static de.dataelementhub.rest.controller.v1.ApiVersion.API_VERSION;

import de.dataelementhub.model.service.Terminologies.UmlsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping(API_VERSION + "/umls")
public class UmlsController {

    private final UmlsService umlsService;

    public UmlsController(UmlsService umlsService) {
        this.umlsService = umlsService;
    }


    @GetMapping("/search")
    public List<UmlsService.UmlsConcept> search(@RequestParam String term) throws IOException {
        return umlsService.searchTerm(term);
    }
}