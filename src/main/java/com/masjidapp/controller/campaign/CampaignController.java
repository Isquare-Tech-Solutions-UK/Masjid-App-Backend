package com.masjidapp.controller.campaign;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CampaignController {

    @GetMapping("/admin/campaign")
    public void getAllCampaigns() {
        // TODO
    }

    @GetMapping("/admin/campaigns/{id}")
    public void getCampaign(@PathVariable("id") UUID id) {
        // TODO
    }

    @PostMapping("/admin/campaigns")
    public void createCampaign() {
        // TODO
    }

    @PutMapping("/admin/campaigns/{id}")
    public void updateCampaign() {
        // TODO
    }

}
