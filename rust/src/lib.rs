pub mod audio_dsp;
pub mod converter;

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jfloat, jint, jstring};
use jni::JNIEnv;

/// Retorna la versión del motor de audio de Rust para Android
#[no_mangle]
pub extern "system" fn Java_com_example_audio_RustAudioBridge_getRustEngineVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = "Rust Audio Engine v0.1.0 (Symphonia + Rubato DSP Architecture)";
    let output = env.new_string(version).expect("Couldn't create java string!");
    output.into_raw()
}

/// Inicializa el pipeline de procesamiento en Rust
#[no_mangle]
pub extern "system" fn Java_com_example_audio_RustAudioBridge_initRustPipeline(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    // Inicialización de buffers de audio y procesador DSP
    1 as jboolean
}

/// Procesa audio a alta velocidad con Rust
#[no_mangle]
pub extern "system" fn Java_com_example_audio_RustAudioBridge_processAudioRust(
    mut env: JNIEnv,
    _class: JClass,
    input_path: JString,
    output_path: JString,
    sample_rate: jint,
    channels: jint,
    volume_gain: jfloat,
) -> jboolean {
    let in_str: String = match env.get_string(&input_path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };
    let out_str: String = match env.get_string(&output_path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };

    let config = converter::ConversionConfig {
        input_path: in_str,
        output_path: out_str,
        target_sample_rate: sample_rate as u32,
        channels: channels as u16,
        volume_multiplier: volume_gain,
    };

    match converter::convert_audio(&config) {
        Ok(_) => 1 as jboolean,
        Err(_) => 0 as jboolean,
    }
}

/// Extrae pistas de audio desde contenedores de video con demuxing en Rust
#[no_mangle]
pub extern "system" fn Java_com_example_audio_RustAudioBridge_extractAudioFromVideoRust(
    mut env: JNIEnv,
    _class: JClass,
    video_path: JString,
    output_path: JString,
    sample_rate: jint,
    channels: jint,
    volume_gain: jfloat,
) -> jboolean {
    let in_str: String = match env.get_string(&video_path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };
    let out_str: String = match env.get_string(&output_path) {
        Ok(s) => s.into(),
        Err(_) => return 0 as jboolean,
    };

    let config = converter::ConversionConfig {
        input_path: in_str,
        output_path: out_str,
        target_sample_rate: sample_rate as u32,
        channels: channels as u16,
        volume_multiplier: volume_gain,
    };

    match converter::extract_audio_from_video(&config) {
        Ok(_) => 1 as jboolean,
        Err(_) => 0 as jboolean,
    }
}
