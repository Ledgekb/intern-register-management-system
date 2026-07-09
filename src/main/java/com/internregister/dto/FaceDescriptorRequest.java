package com.internregister.dto;

import lombok.Data;
import java.util.List;

@Data
public class FaceDescriptorRequest {
    private List<Double> descriptor;
}
