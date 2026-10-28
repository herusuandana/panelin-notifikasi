#!/usr/bin/env python3
"""Compatibility entry point: compact macOS-inspired design replaces the wallet UI."""
from pathlib import Path
import runpy

runpy.run_path(str(Path(__file__).with_name('check-compact-design.py')), run_name='__main__')
