declare module 'openclaw' {
  export class ClawAgent {
    constructor(config: {
      name: string;
      description: string;
      skills: any[];
      model: string;
    });
    process(prompt: string): Promise<any>;
  }

  export interface Skill {
    name: string;
    description: string;
    actions: {
      name: string;
      description: string;
      parameters: Record<string, string | object>;
      handler: (params: any) => Promise<any>;
    }[];
  }
}
