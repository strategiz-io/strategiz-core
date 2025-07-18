package io.strategiz.service.auth.converter;

import io.strategiz.data.auth.entity.passkey.PasskeyCredentialEntity;
import io.strategiz.data.auth.model.passkey.PasskeyCredential;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Converter between PasskeyCredentialEntity and PasskeyCredential domain model
 * Handles conversion between data layer entities and service layer models
 */
@Component
public class PasskeyCredentialConverter {

    /**
     * Convert entity to domain model
     */
    public PasskeyCredential toDomainModel(PasskeyCredentialEntity entity) {
        if (entity == null) {
            return null;
        }

        PasskeyCredential model = new PasskeyCredential();
        model.setId(entity.getId());
        model.setUserId(entity.getUserId());
        model.setCredentialId(entity.getCredentialId());
        model.setPublicKey(entity.getPublicKey());
        model.setPublicKeyBase64(entity.getPublicKeyBase64());
        model.setSignatureCount(entity.getSignatureCount());
        model.setAuthenticatorData(entity.getAuthenticatorData());
        model.setClientDataJSON(entity.getClientDataJSON());
        model.setAttestationObject(entity.getAttestationObject());
        model.setName(entity.getName());
        model.setAuthenticatorName(entity.getAuthenticatorName());
        model.setDeviceName(entity.getDeviceName());
        model.setDevice(entity.getDevice());
        model.setAaguid(entity.getAaguid());
        model.setUserAgent(entity.getUserAgent());
        model.setVerified(entity.getVerified());
        model.setTrusted(entity.isTrusted());
        model.setCreatedAt(entity.getCreatedAt());
        model.setLastUsedAt(entity.getLastUsedAt());
        model.setRegistrationTime(entity.getRegistrationTime());
        model.setLastUsedTime(entity.getLastUsedTime());
        model.setMetadata(entity.getMetadata());

        return model;
    }

    /**
     * Convert domain model to entity
     */
    public PasskeyCredentialEntity toEntity(PasskeyCredential model) {
        if (model == null) {
            return null;
        }

        PasskeyCredentialEntity entity = new PasskeyCredentialEntity();
        entity.setId(model.getId());
        entity.setUserId(model.getUserId());
        entity.setCredentialId(model.getCredentialId());
        entity.setPublicKey(model.getPublicKey());
        entity.setPublicKeyBase64(model.getPublicKeyBase64());
        entity.setSignatureCount(model.getSignatureCount());
        entity.setAuthenticatorData(model.getAuthenticatorData());
        entity.setClientDataJSON(model.getClientDataJSON());
        entity.setAttestationObject(model.getAttestationObject());
        entity.setName(model.getName());
        entity.setAuthenticatorName(model.getAuthenticatorName());
        entity.setDeviceName(model.getDeviceName());
        entity.setDevice(model.getDevice());
        entity.setAaguid(model.getAaguid());
        entity.setUserAgent(model.getUserAgent());
        entity.setVerified(model.getVerified());
        entity.setTrusted(model.isTrusted());
        entity.setLastUsedAt(model.getLastUsedAt());
        entity.setRegistrationTime(model.getRegistrationTime());
        entity.setLastUsedTime(model.getLastUsedTime());
        entity.setMetadata(model.getMetadata());

        return entity;
    }

    /**
     * Convert list of entities to domain models
     */
    public List<PasskeyCredential> toDomainModels(List<PasskeyCredentialEntity> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of domain models to entities
     */
    public List<PasskeyCredentialEntity> toEntities(List<PasskeyCredential> models) {
        if (models == null) {
            return null;
        }
        return models.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * Convert optional entity to optional domain model
     */
    public Optional<PasskeyCredential> toDomainModel(Optional<PasskeyCredentialEntity> entityOpt) {
        return entityOpt.map(this::toDomainModel);
    }

    /**
     * Convert optional domain model to optional entity
     */
    public Optional<PasskeyCredentialEntity> toEntity(Optional<PasskeyCredential> modelOpt) {
        return modelOpt.map(this::toEntity);
    }
}