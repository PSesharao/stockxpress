package com.seshrao.stockxpress.productservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seshrao.stockxpress.productservice.dto.ProductRequest;
import com.seshrao.stockxpress.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers  // for running integration tests
@AutoConfigureMockMvc
class ProductServiceApplicationTests {

	@Container
	static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:5.0.18"));

	@Autowired
	private MockMvc mockMvc ;

	@Autowired
	private ObjectMapper objectMapper ;

	@DynamicPropertySource
	static void setProperties(DynamicPropertyRegistry dynamicPropertyRegistry){
		dynamicPropertyRegistry.add("spring.data.mongodb.uri" , mongoDBContainer::getReplicaSetUrl);
	}

	@Autowired
	ProductRepository productRepository ;

	@Test
	void shouldCreateProduct() throws Exception {

		ProductRequest productRequest = getProductRequest() ;
		String productRequestString = objectMapper.writeValueAsString(productRequest) ;

		mockMvc.perform(MockMvcRequestBuilders.post("/api/product")
				.contentType(MediaType.APPLICATION_JSON)
				.content(productRequestString))
				.andExpect(status().isCreated());

		Assertions.assertEquals(1, productRepository.findAll().size());
	}

	private ProductRequest getProductRequest(){
		return ProductRequest.builder().name("Iphone 13")
				.description("Iphone 13")
				.price(BigDecimal.valueOf(1200))
				.build();
	}

}
