from __future__ import annotations
import pytest
from prism.modules.safety import SafetyModule, SafetyVerdict


def test_clean_input_passes(safety):
    result = safety.check_input("I see an apple")
    assert result.verdict == SafetyVerdict.PASS
    assert result.sanitized == "I see an apple"


def test_blocked_violent_word(safety):
    result = safety.check_input("I want to kill the dragon")
    assert result.verdict == SafetyVerdict.BLOCK
    assert result.sanitized == ""


def test_blocked_output_also_caught(safety):
    result = safety.check_output("There is blood everywhere")
    assert result.verdict == SafetyVerdict.BLOCK


def test_very_long_output_redirected(safety):
    long_text = "This is fine content. " * 30
    result = safety.check_output(long_text)
    assert result.verdict == SafetyVerdict.REDIRECT
    assert len(result.sanitized) < len(long_text)


def test_config_pattern_blocked(config):
    config.safety_blocked_patterns = [r'secret_word']
    s = SafetyModule(config)
    result = s.check_input("The secret_word is here")
    assert result.verdict == SafetyVerdict.BLOCK
