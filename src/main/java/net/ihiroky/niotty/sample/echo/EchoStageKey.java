package net.ihiroky.niotty.sample.echo;

import net.ihiroky.niotty.StageKey;

/**
* @author Hiroki Itoh
*/
enum EchoStageKey implements StageKey {
    LOAD_FRAMING,
    LOAD_DECODE,
    LOAD_APPLICATION,
    STORE_ENCODE,
    STORE_FRAMING,
}
