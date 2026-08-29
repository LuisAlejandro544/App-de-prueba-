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
}
