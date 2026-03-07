package com.edutech.aigateway.application.exception;

import com.edutech.aigateway.domain.model.ModelType;

public class ModelNotFoundException extends AiGatewayException {

    public ModelNotFoundException(ModelType modelType) {
        super("Unknown model type: " + modelType);
    }
}
