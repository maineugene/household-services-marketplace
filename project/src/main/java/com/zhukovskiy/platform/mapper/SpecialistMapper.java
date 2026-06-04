package com.zhukovskiy.platform.mapper;

import com.zhukovskiy.platform.dto.PortfolioItemDto;
import com.zhukovskiy.platform.dto.SpecialistProfileDto;
import com.zhukovskiy.platform.model.PortfolioItem;
import com.zhukovskiy.platform.model.SpecialistProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SpecialistMapper {
    SpecialistMapper INSTANCE = Mappers.getMapper(SpecialistMapper.class);

    @Mapping(target = "portfolio", source = "portfolio")
    SpecialistProfileDto toDto(SpecialistProfile profile);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "moderationStatus", ignore = true)
    @Mapping(target = "isVerified", ignore = true)
    SpecialistProfile toEntity(SpecialistProfileDto dto);

    PortfolioItemDto portfolioToDto(PortfolioItem item);

    PortfolioItem portfolioToEntity(PortfolioItemDto dto);

    List<PortfolioItemDto> portfolioListToDtoList(List<PortfolioItem> items);
}