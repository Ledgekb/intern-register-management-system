package com.internregister.service;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FaceDescriptorService {

    /**
     * Calculates the Euclidean distance between two 128-dimensional face descriptors.
     * Lower distance means higher similarity.
     */
    public double calculateDistance(List<Double> descriptor1, List<Double> descriptor2) {
        if (descriptor1 == null || descriptor2 == null || descriptor1.size() != descriptor2.size()) {
            throw new IllegalArgumentException("Descriptors must be non-null and of the same length.");
        }

        double sum = 0.0;
        for (int i = 0; i < descriptor1.size(); i++) {
            double diff = descriptor1.get(i) - descriptor2.get(i);
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * Verifies if two faces match based on a distance threshold.
     */
    public boolean verify(List<Double> storedDescriptor, List<Double> liveDescriptor, double threshold) {
        double distance = calculateDistance(storedDescriptor, liveDescriptor);
        return distance <= threshold;
    }
}
