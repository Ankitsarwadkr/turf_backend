package com.example.turf_Backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



public class TurfImageResponse {
   public Long getId() {
      return id;
   }

   public void setId(Long id) {
      this.id = id;
   }

   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
   }

   private Long id;
   private String url;



   public TurfImageResponse(Long id, String url) {
      this.id = id;
      this.url = url;
   }
}
