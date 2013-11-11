package net.ihiroky.niotty.sample.echo;

import net.ihiroky.niotty.StageKey;

/**
* @author Hiroki Itoh
*/
enum EchoStageKey implements StageKey {
    FRAMING,
    STRING,
    APPLICATION,
    STORE_ENCODE,
    STORE_FRAMING,
}
