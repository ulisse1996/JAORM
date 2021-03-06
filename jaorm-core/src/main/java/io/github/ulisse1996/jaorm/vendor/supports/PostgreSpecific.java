package io.github.ulisse1996.jaorm.vendor.supports;

import io.github.ulisse1996.jaorm.vendor.specific.DriverType;
import io.github.ulisse1996.jaorm.vendor.specific.LikeSpecific;
import io.github.ulisse1996.jaorm.vendor.specific.LimitOffsetSpecific;
import io.github.ulisse1996.jaorm.vendor.supports.common.PipeLikeSpecific;
import io.github.ulisse1996.jaorm.vendor.supports.common.StandardOffSetLimitSpecific;

public class PostgreSpecific implements LikeSpecific, LimitOffsetSpecific {

    @Override
    public String convertToLikeSupport(LikeType type) {
        return PipeLikeSpecific.INSTANCE.convertToLikeSupport(type);
    }

    @Override
    public DriverType getDriverType() {
        return DriverType.POSTGRE;
    }

    @Override
    public String convertOffSetLimitSupport(int limitRow) {
        return StandardOffSetLimitSpecific.INSTANCE.convertOffSetLimitSupport(limitRow);
    }

    @Override
    public String convertOffSetLimitSupport(int limitRow, int offsetRow) {
        return StandardOffSetLimitSpecific.INSTANCE.convertOffSetLimitSupport(limitRow, offsetRow);
    }
}
