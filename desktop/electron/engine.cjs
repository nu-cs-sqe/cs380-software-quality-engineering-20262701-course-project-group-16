const { createInterface } = require('node:readline');

// The engine owns one table and processes one JSON line at a time.
class Engine {
  constructor(child) {
    this.child = child;
    this.pending = [];
    this.closing = false;
    this.failure = undefined;
    this.closed = new Promise(resolve => child.once('close', resolve));
    const lines = createInterface({ input: child.stdout, crlfDelay: Infinity });
    lines.on('line', line => this.receive(line));
    child.on('error', () => this.fail('The game engine could not start.'));
    child.stdin.on('error', () => this.fail('The game engine connection closed.'));
    child.once('close', () => this.fail('The game engine has stopped.'));
  }

  receive(line) {
    try {
      const reply = JSON.parse(line);
      if (typeof reply.ok !== 'boolean' || !this.pending.length) {
        throw new Error();
      }
      const next = this.pending.shift();
      if (reply.ok) next.resolve(reply.data);
      else next.reject(new Error(reply.error || 'The engine declined the request.'));
    } catch {
      this.fail('The game engine returned an unreadable response.');
      this.child.kill();
    }
  }

  fail(message) {
    this.failure = new Error(message);
    for (const next of this.pending.splice(0)) next.reject(this.failure);
  }

  request(command) {
    if (this.closing || this.failure) {
      return Promise.reject(this.failure || new Error('The game is closing.'));
    }
    const line = JSON.stringify(command) + '\n';
    return new Promise((resolve, reject) => {
      this.pending.push({ resolve, reject });
      this.child.stdin.write(line);
    });
  }

  close() {
    if (!this.closing) {
      this.closing = true;
      this.child.stdin.end();
    }
    return this.closed;
  }
}
module.exports = { Engine };
