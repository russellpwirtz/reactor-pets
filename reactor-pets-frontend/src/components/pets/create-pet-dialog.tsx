'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useCreatePet } from '@/hooks/use-pets';
import { useToast } from '@/hooks/use-toast';

const createPetSchema = z.object({
  name: z.string().min(1, 'Name is required').max(50),
  type: z.enum(['DOG', 'CAT', 'DRAGON']),
});

type CreatePetForm = z.infer<typeof createPetSchema>;

export function CreatePetDialog() {
  const [open, setOpen] = useState(false);
  const createPet = useCreatePet();
  const { toast } = useToast();

  const form = useForm<CreatePetForm>({
    resolver: zodResolver(createPetSchema),
    defaultValues: { name: '', type: 'CAT' },
  });

  const onSubmit = async (data: CreatePetForm) => {
    try {
      await createPet.mutateAsync(data);
      toast({ title: 'Pet created successfully!' });
      setOpen(false);
      form.reset();
    } catch (error) {
      toast({
        title: 'Failed to create pet',
        description: error instanceof Error ? error.message : 'Unknown error',
        variant: 'destructive',
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="lg">Create New Pet</Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Create Your Virtual Pet</DialogTitle>
        </DialogHeader>
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
          <div>
            <Label htmlFor="name">Pet Name</Label>
            <Input
              id="name"
              {...form.register('name')}
              placeholder="Enter a name..."
            />
            {form.formState.errors.name && (
              <p className="text-sm text-red-500">
                {form.formState.errors.name.message}
              </p>
            )}
          </div>

          <div>
            <Label htmlFor="type">Pet Type</Label>
            <Select
              onValueChange={(value) => form.setValue('type', value as 'DOG' | 'CAT' | 'DRAGON')}
              defaultValue="CAT"
            >
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="DOG">Dog</SelectItem>
                <SelectItem value="CAT">Cat</SelectItem>
                <SelectItem value="DRAGON">Dragon</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <Button type="submit" className="w-full" disabled={createPet.isPending}>
            {createPet.isPending ? 'Creating...' : 'Create Pet'}
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}
