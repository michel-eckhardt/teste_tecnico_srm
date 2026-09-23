package com.srm.creditengine.domain.assignor;

import com.srm.creditengine.domain.common.BusinessException;
import com.srm.creditengine.domain.common.ErrorCode;

/** An assignor with the same CNPJ is already registered. */
public class DuplicateAssignorException extends BusinessException {

    public DuplicateAssignorException(String document) {
        super(ErrorCode.DUPLICATE_ASSIGNOR, "Já existe um cedente cadastrado com o CNPJ %s.".formatted(document));
    }
}
