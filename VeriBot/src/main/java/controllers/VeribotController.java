package controllers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import services.ConversationService;

@RestController
@RequestMapping("/api/veribot")
public class VeribotController {
    // GET (ver si hace falta algo mas para mandar respuestas)
    @Autowired
    
	ConversationService Conv;
    @GetMapping(value = "")
    public ResponseEntity<Object> getVeribotResponse(@RequestParam(value = "query", required = true, defaultValue = "") String query) {
        // Now you can use the 'query' parameter that was passed in the URL
        String response = Conv.getResponse(query);
        return new ResponseEntity<Object>(response,HttpStatus.OK);
    }
}
