pub struct DspProcessor {
    pub volume_gain: f32,
}

impl DspProcessor {
    pub fn new(volume_gain: f32) -> Self {
        Self { volume_gain }
    }

    pub fn apply_gain_i16(&self, samples: &mut [i16]) {
        for sample in samples.iter_mut() {
            let amplified = (*sample as f32) * self.volume_gain;
            *sample = amplified.clamp(i16::MIN as f32, i16::MAX as f32) as i16;
        }
    }

    pub fn apply_gain_f32(&self, samples: &mut [f32]) {
        for sample in samples.iter_mut() {
            *sample = (*sample * self.volume_gain).clamp(-1.0, 1.0);
        }
    }

    /// Procesa audio 8D espacial con rotación orbital continua
    pub fn apply_spatial_8d(
        &self,
        samples: &[i16],
        output: &mut Vec<i16>,
        sample_rate: u32,
        src_channels: u16,
        rotation_speed_hz: f32,
        spatial_depth: f32,
    ) {
        let frames = if src_channels == 2 { samples.len() / 2 } else { samples.len() };
        output.clear();
        output.reserve(frames * 2);

        let phase_inc = (2.0 * std::f32::consts::PI * rotation_speed_hz) / (sample_rate as f32);
        let mut phase = 0.0f32;

        for f in 0..frames {
            let (in_l, in_r) = if src_channels == 2 {
                (samples[f * 2] as f32 / 32768.0, samples[f * 2 + 1] as f32 / 32768.0)
            } else {
                let mono = samples[f] as f32 / 32768.0;
                (mono, mono)
            };

            let azimuth = phase;
            phase += phase_inc;
            if phase >= 2.0 * std::f32::consts::PI {
                phase -= 2.0 * std::f32::consts::PI;
            }

            let sin_azimuth = azimuth.sin();
            let pan_normalized = ((sin_azimuth * spatial_depth + 1.0) * 0.5).clamp(0.0, 1.0);

            let gain_left = (pan_normalized * std::f32::consts::FRAC_PI_2).cos();
            let gain_right = (pan_normalized * std::f32::consts::FRAC_PI_2).sin();

            let out_l = ((in_l * gain_left * self.volume_gain) * 32767.0).clamp(-32768.0, 32767.0) as i16;
            let out_r = ((in_r * gain_right * self.volume_gain) * 32767.0).clamp(-32768.0, 32767.0) as i16;

            output.push(out_l);
            output.push(out_r);
        }
    }
}
