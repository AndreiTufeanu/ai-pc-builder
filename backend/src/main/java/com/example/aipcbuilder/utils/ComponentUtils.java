package com.example.aipcbuilder.utils;

import com.example.aipcbuilder.model.PcComponent;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@NoArgsConstructor
public class ComponentUtils {

    public boolean isValidComponentRequest(Map<String, Object> requestData) {
        return requestData.containsKey("name") &&
                !requestData.get("name").toString().trim().isEmpty() &&
                requestData.containsKey("type");
    }

    public PcComponent createComponentFromRequest(Map<String, Object> requestData) {
        PcComponent component = new PcComponent();
        component.setName(requestData.get("name").toString());
        component.setType(PcComponent.ComponentType.valueOf(requestData.get("type").toString()));

        setOptionalFields(component, requestData);
        return component;
    }

    public void updateComponentFromRequest(PcComponent component, Map<String, Object> requestData) {
        if (requestData.containsKey("name")) component.setName(requestData.get("name").toString());
        if (requestData.containsKey("type")) component.setType(PcComponent.ComponentType.valueOf(requestData.get("type").toString()));

        setOptionalFields(component, requestData);
    }

    private void setOptionalFields(PcComponent component, Map<String, Object> requestData) {
        if (requestData.containsKey("description")) component.setDescription(requestData.get("description").toString());
        if (requestData.containsKey("manufacturer")) component.setManufacturer(requestData.get("manufacturer").toString());
        if (requestData.containsKey("model")) component.setModel(requestData.get("model").toString());

        if (requestData.containsKey("price") && requestData.get("price") != null) {
            try {
                Double priceValue = Double.parseDouble(requestData.get("price").toString());
                component.setPriceFromDouble(priceValue);
            } catch (NumberFormatException e) {
                component.setPrice(null);
            }
        }

        if (requestData.containsKey("specifications") && requestData.get("specifications") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> specifications = (Map<String, Object>) requestData.get("specifications");
            component.setSpecifications(specifications);
        }
    }
}