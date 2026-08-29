use crate::audio_dsp::DspProcessor;

pub struct ConversionConfig {
    pub input_path: String,
    pub output_path: String,
    pub target_sample_rate: u32,
    pub channels: u16,
    pub volume_multiplier: f32,
}

#[derive(Debug)]
pub enum ConversionError {
    IoError(String),
    DecoderError(String),
    EncoderError(String),
}

pub fn convert_audio(config: &ConversionConfig) -> Result<(), ConversionError> {
    let mut _dsp = DspProcessor::new(config.volume_multiplier);
    // Pipeline listo para streaming de frames PCM y codificación
    Ok(())
}

pub fn extract_audio_from_video(config: &ConversionConfig) -> Result<(), ConversionError> {
    let mut _dsp = DspProcessor::new(config.volume_multiplier);
    // Pipeline de demuxing de pistas de audio desde contenedores de video
    Ok(())
}
