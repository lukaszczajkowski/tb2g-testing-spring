package org.springframework.samples.petclinic.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@SpringJUnitWebConfig(locations = {"classpath:spring/mvc-test-config.xml", "classpath:spring/mvc-core-config.xml"})
class OwnerControllerTest {

    @Autowired
    OwnerController ownerController;

    @Autowired
    ClinicService clinicService;

    MockMvc mockMvc;

    @Captor
    ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    ArgumentCaptor<Owner> ownerArgumentCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ownerController).build();
    }

    @AfterEach
    void tearDown() {
        // this is needed because the bean in the test context is being reused, so we invoke the same
        // method more than once
        reset(clinicService);
    }

    @Test
    void testNewOwnerPostValid() throws Exception {
        mockMvc.perform(post("/owners/new")
                    .param("firstName", "Jimmy")
                    .param("lastName", "Buffet")
                    .param("address", "123 Duval St")
                    .param("city", "Key West")
                    .param("telephone", "3211231234"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void testNewOwnerPostNotValid() throws Exception {
        mockMvc.perform(post("/owners/new")
                    .param("firstName", "Jimmy")
                    .param("lastName", "Buffet")
                    .param("city", "Key West"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("owner"))
                .andExpect(model().attributeHasFieldErrors("owner", "address"))
                .andExpect(model().attributeHasFieldErrors("owner", "telephone"))
                .andExpect(view().name("owners/createOrUpdateOwnerForm"));
    }

    @Test
    void shouldUpdateOwnerForm() throws Exception {
        final String firstName = "Jimmy";
        final String lastName = "Buffet";
        final String address = "123 Duval St";
        final String city = "Key West";
        final String telephone = "3211231234";
        Owner owner = new Owner();
        owner.setId(1);
        owner.setFirstName(firstName);
        owner.setLastName(lastName);
        owner.setAddress(address);
        owner.setCity(city);
        owner.setTelephone(telephone);

        mockMvc.perform(post("/owners/{ownerId}/edit", 1)
                    .param("firstName", firstName)
                    .param("lastName", lastName)
                    .param("address", address)
                    .param("city", city)
                    .param("telephone", telephone))
                .andExpect(status().is3xxRedirection());

        then(clinicService).should().saveOwner(ownerArgumentCaptor.capture());
        assertThat(ownerArgumentCaptor.getValue()).isNotNull();
        assertThat(ownerArgumentCaptor.getValue().getId()).isEqualTo(owner.getId());
    }

    @Test
    void shouldHaveErrorsWhileUpdatingForm() throws Exception {
        mockMvc.perform(post("/owners/{ownerId}/edit", 1)
                    .param("firstName", "Jimmy")
                    .param("lastName", "Buffet")
                    .param("city", "Key West"))
                .andExpect(status().isOk())
                .andExpect(view().name("owners/createOrUpdateOwnerForm"))
                .andExpect(model().attributeHasErrors("owner"))
                .andExpect(model().attributeHasFieldErrors("owner", "telephone"))
                .andExpect(model().attributeHasFieldErrors("owner", "address"));
    }

    @Test
    void initCreationFormTest() throws Exception {
        mockMvc.perform(get("/owners/new"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("owner"))
                .andExpect(view().name("owners/createOrUpdateOwnerForm"));
    }

    @Test
    void testFindByNameNotFound() throws Exception {
        mockMvc.perform(get("/owners")
                .param("lastName", "DontFindMe"))
                .andExpect(status().isOk())
                .andExpect(view().name("owners/findOwners"));
    }

    @Test
    void shouldReturnOneOwner() throws Exception {
        // given
        final String found = "Found";
        Owner owner = new Owner();
        owner.setLastName(found);
        owner.setId(1);
        given(clinicService.findOwnerByLastName(found))
                .willReturn(List.of(owner));

        mockMvc.perform(get("/owners")
                .param("lastName", found))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/owners/1"));

        then(clinicService).should().findOwnerByLastName(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getValue()).isEqualToIgnoringCase(found);
    }

    @Test
    void shouldReturnListOfOwners() throws Exception {
        given(clinicService.findOwnerByLastName("")).willReturn(List.of(new Owner(), new Owner()));

        mockMvc.perform(get("/owners"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("selections"))
                .andExpect(view().name("owners/ownersList"));

        then(clinicService).should().findOwnerByLastName(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getValue()).isEqualToIgnoringCase("");
    }
}