package com.example.turf_Backend.service;


import com.example.turf_Backend.dto.response.PublicTurfCardResponse;
import com.example.turf_Backend.dto.response.PublicTurfDetailsResponse;
import com.example.turf_Backend.entity.Turf;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.PublicTurfMapper;
import com.example.turf_Backend.repository.TurfRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicTurfService {

    private final TurfRepository turfRepository;
    private final PublicTurfMapper publicTurfMapper;
    public List<PublicTurfCardResponse> listAllTurfs() {
    List<Turf> turfs=turfRepository.findByAvailableTrue();
    return turfs.stream()
            .map(publicTurfMapper::toCard)
            .toList();

    }

    public PublicTurfDetailsResponse getTurfDetails(Long turfId) {
        Turf turf=turfRepository.findById(turfId)
                .orElseThrow(()->new CustomException("Turf Not Found", HttpStatus.NOT_FOUND));

        if (!turf.isAvailable())
        {
            throw new CustomException("Turf is Unavailable",HttpStatus.BAD_REQUEST);
        }
        return publicTurfMapper.toTurfDetails(turf);
    }
}
